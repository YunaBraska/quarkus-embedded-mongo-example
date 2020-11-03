package berlin.yuna.quarkus.mongodb.util;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.awaitility.Awaitility.await;


public class EmbeddedMongoDb implements QuarkusTestResourceLifecycleManager {
    private static MongodExecutable mongoExe;
    private static MongodProcess process;

    private static final MongodStarter starter = MongodStarter.getInstance(prepareConfig());
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(EmbeddedMongoDb.class);

    @Override
    public Map<String, String> start() {
        final HashMap<String, String> map = new HashMap<>();
        final String host = "localhost";
        final int port = findPort(host, 27017);
        map.put("embedded.mongodb.host", host);
        map.put("embedded.mongodb.port", String.valueOf(port));
        map.put("quarkus.mongodb.write-concern.journal", "false");
        map.put("quarkus.mongodb.database", "embedded");
        map.put("quarkus.mongodb.connection-string", "mongodb://" + host + ":" + port);
        try {
            startUp(host, port);
            LOG.info("Started embedded mongoDb");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private int findPort(final String host, final int defaultPort) {
        if (isPortInUse(host, defaultPort)) {
            final Random random = new Random();
            for (int i = 0; i < 64; i++) {
                final int port = random.nextInt(500) + 5000;
                if (!isPortInUse(host, port)) {
                    return port;
                }
            }
            throw new IllegalStateException("Could not find any free port");
        }
        return defaultPort;
    }

    @Override
    public void stop() {
        try {
            tearDown();
        } catch (Exception e) {
            LOG.info("Stopped embedded mongoDb");
            throw new RuntimeException(e);
        }
    }

    public static void startUp(String hostName, int portNumber) throws Exception {
        mongoExe = starter.prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(hostName, portNumber, Network.localhostIsIPv6()))
                .build());
        process = mongoExe.start();

        await().atMost(10, TimeUnit.SECONDS).until(() -> isPortInUse(hostName, portNumber));
        LOG.info("Started embedded mongoDb");
    }

    public static void tearDown() throws Exception {
        process.stop();
        mongoExe.stop();
        LOG.info("Stopped embedded mongoDb");
    }

    private static boolean isPortInUse(String hostName, int portNumber) {
        try {
            new Socket(hostName, portNumber).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static IRuntimeConfig prepareConfig() {
        Level logLevel = Level.OFF;
        Logger logger = Logger.getLogger(EmbeddedMongoDb.class.getName());
        ProcessOutput processOutput = new ProcessOutput(
                Processors.logTo(logger, logLevel),
                Processors.logTo(logger, Level.SEVERE),
                Processors.named("[console>]", Processors.logTo(logger, logLevel))
        );

        return new RuntimeConfigBuilder()
                .defaultsWithLogger(Command.MongoD, logger)
                .processOutput(processOutput)
                .artifactStore(new ExtractedArtifactStoreBuilder()
                        .defaults(Command.MongoD)
                        .download(new DownloadConfigBuilder()
                                .defaultsForCommand(Command.MongoD)
                                .progressListener(new LoggingProgressListener(logger, logLevel))))
                .build();
    }
}
