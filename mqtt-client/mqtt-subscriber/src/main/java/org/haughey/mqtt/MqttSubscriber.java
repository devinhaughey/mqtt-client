package org.haughey.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Subscriber client that connects to broker and receives messages from a
 * specific topic or wildcard with MQTT.
 *
 * @author dhaugh
 */
@SpringBootApplication
@IntegrationComponentScan
@EnableAutoConfiguration
@Component
@ComponentScan(basePackages = {"org.haughey.mqtt"})
// TODO: Update code with a ConfigDefaultVariables Properties file
public class MqttSubscriber  {

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

    @Value("${numMessages:1000}")
    private long numMessages = 0;

    public long count = 0;

    /**
     * Main method starts the Subscriber client that establishes
     * a connection to the broker and receive numMessages from a subscribed
     * topic.
     *
     * @param args CLA if default need to be different
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(MqttSubscriber.class)
                .web(false)
                .run(args);
    }

    /**
     * MessageChannel method creates a new mqttInputChannel
     * to connect to the Broker with a single threaded connection.
     * Downstream components are connected via Direct Channel.
     *
     * @return   DirectChannel single threaded connection
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MqttPahoClientFactory method establishes the URL of the server along with the host and
     * port, the username, and the password for connecting to the determined broker.
     * Generates the Last Will and Testament for the publisher clientId
     * for a lost connection situation. SSL connection to the broker is possible with
     * the correct keyStore and trustStore provided.
     *
     * @return   factory with given variables
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        java.util.Properties sslClientProps = new java.util.Properties();

      /*sslClientProps.setProperty("com.ibm.ssl.keyStore","src/main/resources/client.ks");
        sslClientProps.setProperty("com.ibm.ssl.keyStorePassword","password");

        sslClientProps.setProperty("com.ibm.ssl.trustStore","src/main/resources/client.ts");
        sslClientProps.setProperty("com.ibm.ssl.trustStorePassword","password");*/

        // TODO: Provide better logic in case user uses something besides the default MQTT ports
        if (port == 1883){
            factory.setServerURIs("tcp://" + host + ":" + port);
        } else{
            factory.setServerURIs("ssl://" + host + ":" + port);
        }

        factory.setUserName(user);
        factory.setPassword(password);
        factory.setSslProperties(sslClientProps);
        factory.setWill(new DefaultMqttPahoClientFactory.Will(topic, "I have died...".getBytes(), qos, true ));
        return factory;
    }

    /**
     * MessageProducer generates a clientID for the subscriber by using a randomUUID
     * for creating a connection to a broker. Creates a new
     * connection adapter to a broker by setting the clientID, MqttClientFactory,
     * topic given to subscribe to, Completion Timeout, Converter,
     * Qos, and the Output Channel for numMessages to go to.
     *
     * @return   adapter for the mqttInbound connection
     */
    @Bean
    public MessageProducer mqttInbound() {
        if (clientId.equals("clientTest")){
            clientId = UUID.randomUUID().toString();
            System.out.println(clientId);
        }

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory(), topic);

        adapter.setCompletionTimeout(1000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * Message handler calculates the amount of messages that have come in
     * from the subscribed topic and posts the result.
     *
     * @return   message is the numMessages amount
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {

            if( count % 10 == 0 ) {
                System.out.println(String.format("Received %d numMessages from " + topic + " topic.", count));
            }
            if (count == numMessages)
            {
                System.exit(0);
            }
            count ++;
            //System.out.println(message.getPayload());
        };
    }

    /**
     * Creates a new instance of the PropertySourcesPlaceholderConfigurer
     * to pass property sources from the application.properties file.
     *
     * @return   PropertySourcesPlaceholderConfigurer new instance
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}