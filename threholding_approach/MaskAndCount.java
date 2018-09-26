import ij.ImagePlus;

public class MaskAndCount {
	public ImagePlus mask;
	public int count;
	
	public MaskAndCount(ImagePlus mask,int count){
		this.mask = mask;
		this.count = count;
	}
}
