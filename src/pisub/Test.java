package pisub;

public class Test {

	public static void main(String[] args) {
		String topic = "1/pi3/pub/temp/in";
		
		System.out.println("Index = " + topic.indexOf("i3"));
		System.out.println(topic.substring(0, topic.indexOf("i3") + 3));
		
		String subTopic = topic.substring(0, topic.indexOf("i3") + 3) + "sub";
		System.out.println("Sub Topic = " + subTopic);
		
		subTopic = topic.substring(0, topic.indexOf("in")) + "out";
		System.out.println(subTopic);

	}

}
