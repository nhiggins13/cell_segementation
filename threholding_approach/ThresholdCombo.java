import java.io.Serializable;

public class ThresholdCombo implements Serializable {
	public String name;
	public double accuracy;
	public double difference;
	public double ji;
	
	public ThresholdCombo(String name,double accuracy,double difference,double ji){
		this.name = name;
		this.accuracy = accuracy;
		this.difference = difference;
		this.ji = ji;
	}
}
