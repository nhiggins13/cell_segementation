
public class AccuracyMetric {
	public double TP = 0.0;
	public double TN = 0.0;
	public double FP = 0.0;
	public double FN = 0.0;
	public double TPR;
	public double FPR;
	public int diff;
	
	public AccuracyMetric(double TP,double TN, double FP, double FN){
		this.TP =TP;
		this.TN = TN;
		this.FP = FP;
		this.FN = FN;
		
		
		TPR = TP/(TP+FN);
		FPR = FP/(FP+TN);
	}
}
