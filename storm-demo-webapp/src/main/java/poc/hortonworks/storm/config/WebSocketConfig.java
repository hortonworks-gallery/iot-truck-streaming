package poc.hortonworks.storm.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
@EnableScheduling
@ComponentScan(basePackages="poc.hortonworks.storm")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/monitor").withSockJS();
		registry.addEndpoint("/streamgenerator").withSockJS();
		registry.addEndpoint("/resetdemo").withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		Properties properties = new Properties();
		try{
			properties.load(new FileInputStream(new File("/etc/storm_demo/config.properties")));
		}
		catch(Exception ex){
			System.err.println("Unable to locate config file: /etc/storm_demo/config.properties");
			System.exit(0);
		}
		String server = properties.getProperty("activemq.server");
		Integer port = new Integer(properties.getProperty("activemq.port"));
		
		StompBrokerRelayRegistration registration = registry.enableStompBrokerRelay("/queue", "/topic");
		registration.setRelayHost(server);
		registration.setRelayPort(port);
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration inboundChannel) {
		
	}

	@Override
	public void configureClientOutboundChannel(ChannelRegistration out) {
		
	}

}
