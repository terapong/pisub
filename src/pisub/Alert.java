package pisub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class Alert implements Runnable {
	private MqttClient myClient;
	private String topic;
	private String pubMsg = "ALERT";
	private int qos = 0;
	
	
	
	public Alert(MqttClient myClient, String topic) {
		this.myClient = myClient;
		this.topic = topic;
	}
	
	private void sendControl() {
		// setup MQTT Client
		MqttMessage pubMessage = new MqttMessage(pubMsg.getBytes());
		pubMessage.setQos(qos);
		pubMessage.setRetained(false);
		
		
		MqttDeliveryToken token = null;
		MqttTopic topicControl = myClient.getTopic(topic);
		try {
			// publish message to broker
			System.out.println("Publishing to topic \"" + topic + "\" qos " + qos + " Message " + pubMsg);
			token = topicControl.publish(pubMessage);
			// Wait until the message has been delivered to the broker
			token.waitForCompletion();
			Thread.sleep(1000);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		sendControl();
	}
}
