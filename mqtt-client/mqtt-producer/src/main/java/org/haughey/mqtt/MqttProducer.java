package org.haughey.mqtt;

import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.UUID;

/**
 * MqttProducer MQTT creates numMessages that are sent to a topic set here by the user
 * or by a default one to a remote broker.
 *
 * @author dhaugh
 */
@SpringBootApplication
@IntegrationComponentScan
@EnableAutoConfiguration
@Component
// TODO: Update code with a ConfigDefaultVariables Properties file
public class MqttProducer {

    @Value("${user:admin}")
    private String user;

    @Value("${password:admin}")
    private String password;

    @Value("${clientId:clientTest}")
    private String clientId;

    @Value("${host:localhost}")
    private String host;

    @Value("${port:1883}")
    private int port;

    @Value("${waitTime:5}")
    private int waitTime;

    @Value("${qos:1}")
    private int qos;

    @Value("${topic:testTopic}")
    private String topic;

    @Value("${numMessages:100}")
    private int numMessages;

    /**
     * The application takes in command line arguments or values provided
     * by a property file. There are set default values for the variables
     * if non are given. The program sends a message a 1000 times or the
     * given amount by the user to the topic chosen by the user to the
     * broker. Once numMessages hits its max, the application will
     * disconnect from the broker and exit.
     *
     * @param args
     *          Commandline arguments passed
     * @throws IOException
     *          Thrown if cannot read the file
     */
    public static void main(String[] args) throws IOException {

        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(MqttProducer.class)
                        .web(false)
                        .run(args);
        MyGateway gateway = context.getBean(MyGateway.class);

        // TODO: Refactor code to fix usage of static variables by SpringBoot so values can be passed
        int numMessages = 100;

        MqttProducer messageData = new MqttProducer();

        for( int i=1; i <= numMessages; i ++) {
            // Sleep is performed in Milliseconds, 10s
            try {
                Thread.sleep(4500);
                gateway.sendToMqtt(messageData.readFileIn());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if( i % 10 == 0 ) {
                System.out.println(String.format("Sent %d numMessages.", i));
            }
        }

        System.exit(0);
    }

    /**
     * Method readFileIn() reads the given text file into a String so that it can
     * be sent to the broker.
     *
     * @return messageData as a String
     * @throws IOException if file isn't found then IOException is thrown
     */
    private String readFileIn() throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/testfilelarge.txt")));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        String messageData = sb.toString();
        //System.out.println(messageData);

        return messageData;
    }

    /**
     * Creates a MQTT Client factory with credentials to connect to a broker, if
     * using SSL then uses the provided ClientID with keystore and truststore
     * passwords. Provided is the last will and testament for the situation of
     * a loss connection to the broker.
     *
     * @return factory with the provided credentials to connect to the broker
     */
    @Bean
    private MqttPahoClientFactory mqttClientFactory() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        java.util.Properties sslClientProps = new java.util.Properties();
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        /* If using SSL to connect, provide the keystore and truststore files in resources
        sslClientProps.setProperty("com.ibm.ssl.keyStore","src/main/resources/clientId.ks");
        sslClientProps.setProperty("com.ibm.ssl.keyStorePassword","password");

        sslClientProps.setProperty("com.ibm.ssl.trustStore","src/main/resources/clientId.ts");
        sslClientProps.setProperty("com.ibm.ssl.trustStorePassword","password");
        */

        // TODO: Provide better logic in case user uses something besides the default MQTT ports
        if (port == 1883){
            factory.setServerURIs("tcp://" + host + ":" + port);
            System.out.println("tcp://" + host + ":" + port);
        } else{
            factory.setServerURIs("ssl://" + host + ":" + port);
            System.out.println("ssl://" + host + ":" + port);
        }

        factory.setUserName(user);
        factory.setPassword(password);
        factory.setSslProperties(sslClientProps);
        factory.setWill(new DefaultMqttPahoClientFactory.Will(topic, "I have died...".getBytes(), qos, true ));
        factory.setPersistence(dataStore);
        factory.setKeepAliveInterval(5000);
        //factory.setPersistence(new MemoryPersistence());
        return factory;
    }

    /**
     * Generates the MessageHandler for setting the clientId, topic,
     * quality of service and the completion timeout. It utilizes the mqttClientFactory()
     * to connect to the broker with the provided credentials.
     *
     * @return messageHandler sets the connection variables
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        if (clientId.equals("clientTest")){
            clientId = UUID.randomUUID().toString();
            System.out.println(clientId);
        }
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId, mqttClientFactory());

        // The caller will not block messages waiting for a delivery confirmation when
        // a message is sent so that the message can be received. Default false
        //messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(topic);
        messageHandler.setDefaultQos(qos);
        messageHandler.setCompletionTimeout(50000);
        return messageHandler;
    }

    /**
     * Generates a new MessageChannel for sending messages until the
     * numMessages reaches its max to the broker.
     *
     * @return DirectChannel connection type to the broker
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MyGateway sends the created message string to
     * the broker with sendToMqtt().
     */
    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    private interface MyGateway {

        void sendToMqtt(String data);
    }
    // TODO: Get property file to work
    /**
     * Property file that includes default values used by SpringBoot to
     * be passed if none are provided by the user through commandline.
     *
     * @return PropertySourcesPlaceholderConfigurer() new instance
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
