package blueprint.docker.mq.producer;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dataset.DataSet;
import org.apache.camel.component.dataset.SimpleDataSet;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static int interval;

    private static String queueName;

    private static int messageSize;

    private static long messageCount;

    private static String brokerUrl;

    public static void main(String args[]) {
        new Main().run();
    }

    public void run() {
        try {
            try {
                queueName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_QUEUE_NAME");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_QUEUE_NAME", "TEST.FOO") : result;
                        return result;
                    }
                });

                brokerUrl = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_BROKER_URL");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_BROKER_URL", "TEST.FOO") : result;
                        return result;
                    }
                });

                String intervalStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_INTERVAL");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_INTERVAL", "0") : result;
                        return result;
                    }
                });
                if (intervalStr != null && intervalStr.length() > 0) {
                    interval = Integer.parseInt(intervalStr);
                }

                String messageSizeInBytesStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_MESSAGE_SIZE_BYTES");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_MESSAGE_SIZE_BYTES", "1024") : result;
                        return result;
                    }
                });
                if (messageSizeInBytesStr != null && messageSizeInBytesStr.length() > 0) {
                    messageSize = Integer.parseInt(messageSizeInBytesStr);
                }

                String messageCountStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_MESSAGE_COUNT_LONG");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_MESSAGE_COUNT_LONG", "10000") : result;
                        return result;
                    }
                });
                if (messageCountStr != null && messageCountStr.length() > 0) {
                    messageCount = Long.parseLong(messageCountStr);
                }

            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (queueName == null) {
                queueName = "TEST.FOO";
            }

            if (messageSize <= 1) {
                messageSize = 1024;
            }

            if (messageCount <= 0) {
                messageCount = 10000;
            }


            SimpleRegistry registry = new SimpleRegistry();
            CamelContext context = new DefaultCamelContext(registry);

            registry.put("myDataSet", createDataSet());

            ActiveMQComponent amq = new ActiveMQComponent();
            LOG.info("Setting broker url to {}", brokerUrl);
            amq.setBrokerURL(brokerUrl);
            context.addComponent("dactivemq", amq);

            context.addRoutes(new RouteBuilder() {
                public void configure() {
                    from("dataset:myDataSet?produceDelay=" + interval).log("Sending data to queue " + queueName).to("dactivemq:" + queueName);
                }
            });

            // I couldn't figure out how to add a component
            // to the camel Main class, so i've taken wait
            // until completed from it and used context directly
            context.start();
            waitUntilCompleted();
            context.stop();

        } catch (Throwable e) {
            LOG.error("Failed to connect to Fabric8 MQ", e);
        }
    }

    private CountDownLatch latch = new CountDownLatch(1);
    private AtomicBoolean completed = new AtomicBoolean(false);
    private long duration = -1;

    protected void waitUntilCompleted() {
        while (!completed.get()) {
            try {
                if (duration > 0) {
                    LOG.info("Waiting for: " + duration + " " + TimeUnit.MILLISECONDS);
                    latch.await(duration, TimeUnit.MILLISECONDS);
                    completed.set(true);
                } else {
                    latch.await();
                }
            } catch (InterruptedException e) {
                LOG.debug("Caught: " + e);
            }
        }
    }

    static DataSet createDataSet() {
        char[] chars = new char[messageSize];
        Arrays.fill(chars, 'a');

        String messageBody = new String(chars);

        SimpleDataSet dataSet = new SimpleDataSet();
        dataSet.setSize(messageCount);
        dataSet.setDefaultBody(messageBody);

        return dataSet;
    }

    public static int getInterval() {
        return interval;
    }

    public static String getQueueName() {
        return queueName;
    }

    public static int getMessageSize() {
        return messageSize;
    }

    public static long getMessageCount() {
        return messageCount;
    }

}
