import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

public class Test {
	private String[] thresholds = {"Default",
									"Huang",
									"Intermodes",
									"IsoData",
									"Li",
									"MaxEntropy",
	                               "Mean",
									"Minimum",
									"Moments","Otsu",
									"Percentile",
									"RenyiEntropy",
	                               "Shanbhag",
									"Triangle"
									};
	
	private String[] locThresholds = {"Bernsen","Contrast","Mean","Median","MidGrey","Niblack","Otsu",
										"Phansalkar","Sauvola"
										};
	
	private String imageDir;
	private String saveDir;
	
	private Hashtable<String,ThresholdCombo> thresCombos = new Hashtable<String,ThresholdCombo>();
	private Hashtable<String,MaskAndCount> goldStandards = new Hashtable<String,MaskAndCount>();
	
	private File dir;
	private File[] directoryListing;
	
	
	/**
	 * 
	 * @param imageDirectory
	 * @param saveDirectory
	 * 
	 * Set up test object
	 */
	public Test(String imageDirectory,String saveDirectory){
		imageDir = imageDirectory;
		saveDir = saveDirectory;
		
		dir = new File(imageDir);
		directoryListing = dir.listFiles();
		
		if (directoryListing != null) {//check that directory has files
			for (int i =0;i<directoryListing.length;i++) {//loop through files
				String fileName = directoryListing[i].getName();
				
				ImagePlus image = IJ.openImage(imageDir+fileName);
				MaskAndCount mac = convertToMask(image,false);
				
				goldStandards.put(fileName, mac);
				
			}
		}
		
	}
	
	/**
	 * 
	 * @param image
	 * @param show
	 * @return an image representing the mask of the rois
	 * 
	 * Creates a mask of the rois given image
	 */
	public MaskAndCount convertToMask(ImagePlus image,boolean show){
		//add overlays to roi manager
		IJ.run(image,"To ROI Manager", "");
		
		//get roi manager
		RoiManager roiMng = RoiManager.getInstance();
		
		
		//create mask of image
		ImagePlus impMask = IJ.createImage("Mask", "8-bit grayscale-mode", image.getWidth(), image.getHeight(), image.getNChannels(), image.getNSlices(), image.getNFrames());
	    IJ.setForegroundColor(255, 255, 255);
	    if(roiMng.getCount()>0){
		    roiMng.runCommand(impMask,"Deselect");
		    roiMng.runCommand(impMask,"Fill");
	    }
	    //show mask if wanted
	    if(show == true){
	    	impMask.show();
	    }
	    
	    MaskAndCount mac = new MaskAndCount(impMask,roiMng.getCount());
	    roiMng.reset();
		return mac;
	}
	
	/**
	 * 
	 * @param gold
	 * @param seg
	 * @return object containing accuracy metrisc
	 * 
	 * Creates and returns an object contain metrics such as true positive rate
	 */
	public AccuracyMetric getMetrics(ImagePlus gold,ImagePlus seg){
		//counters
		double TP = 0.0;
		double TN = 0.0;
		double FP = 0.0;
		double FN = 0.0;
		
		//loop through pixels
		for(int i =0;i<gold.getHeight();i++){
			for(int j = 0; j<gold.getWidth();j++){
				
				//keep track of the positve and negative rates
				if(gold.getPixel(i, j)[0]==seg.getPixel(i, j)[0]){
					
					if(gold.getPixel(i, j)[0]==0){
						TN++;
					}
					else{
						TP++;
					}
				}
				else{
					if(gold.getPixel(i, j)[0]==0){
						FP++;
					}
					else{
						FN++;
					}
				}
			}
				
		}
		
		return new AccuracyMetric(TP,TN,FP,FN);
	}
	
	/**
	 * 
	 * @param list
	 * @return the mean of a list
	 * 
	 * returns the average for a given double list
	 */
	public double mean(Double[] list){
		double sum = 0.0;
		for(double d:list){
			sum += d;
		}
		return sum/list.length;
	}
	
	/**
	 * 
	 * @param filepath
	 * @param arraylist
	 * 
	 * saves a Segmentnuclei object
	 */
	public void save(String filepath,ArrayList<ThresholdCombo> arraylist){
		try
	      {
	         FileOutputStream fileOut = new FileOutputStream(filepath);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(arraylist);
	         out.close();
	         fileOut.close();
	      }
	      catch(IOException i)
	      {
	          i.printStackTrace();
	      }
	}
	/**
	 * 
	 * @return arraylist of the pareto threshold combinations
	 * 
	 * loops through all combinations of thresholds and returns a list of objects
	 *  containing the average evalutaion. All objects are pareto combinations
	 */
	public ArrayList<ThresholdCombo> thresholdCombos(){
		//get list of image files
		File dir = new File(imageDir);
		File[] directoryListing = dir.listFiles();
		Double[] accs = new Double[directoryListing.length];//array of accuracy
		Double[] diffs = new Double[directoryListing.length];//array of differences
		Double[] jis = new Double[directoryListing.length];//arrray of Jaccard indexes
		SegmentNuclei sn = new SegmentNuclei();
		
		for (String threshold : thresholds){
			for(String locThreshold : locThresholds){
				
				String key = threshold+"-"+locThreshold;//create key name
				System.out.println(key);
				
				if (directoryListing != null) {//check that directory has files
					for (int i =0;i<directoryListing.length;i++) {//loop through files
						String fileName = directoryListing[i].getName();
						String savePath = saveDir+key+"-"+fileName;
						
						//segment image
						sn.createSegmentedImage(imageDir+fileName, savePath, "H&E", threshold, locThreshold, false);
						
						//create masks
						ImagePlus seg = IJ.openImage(savePath);
						MaskAndCount segMac = convertToMask(seg,false);
						MaskAndCount goldMac = goldStandards.get(fileName);
						
						//get accuracy metrics like TPR and FPR
						AccuracyMetric am = getMetrics(goldMac.mask,segMac.mask);
						Double acc = (am.TP+am.TN)/(am.TP+am.TN+am.FP+am.FN);
						Double ji =  (am.TP)/(am.FN+am.FP+am.TP);
						
						//store metrics
						accs[i] = acc;
						diffs[i] = (double) Math.abs(goldMac.count-segMac.count);
						jis[i] = ji;
						IJ.run("Close All");
					}
				}
				
				//store combinations
				ThresholdCombo combo = new ThresholdCombo(key,mean(accs),mean(diffs),mean(jis));
				thresCombos.put(key,combo);
				
				
				
				
			}
		}
		
		ArrayList<String> keys = Collections.list(thresCombos.keys());//get keys
		ArrayList<ThresholdCombo> pareto = new ArrayList<ThresholdCombo>();//list for storing best combos
		
		System.out.println("\n\nStats on all Threshold Combos");
		for(int i =0;i<keys.size();i++) {//loop through keys
			
			
		    String key = keys.get(i);// get key
		    ThresholdCombo tc = thresCombos.get(key);//get combo
		    
		    System.out.println(tc.name+"{ "+"Accuracy: "+tc.accuracy+", Difference: "+tc.difference);
		    
		    Boolean flag = true;
		    for(int j =0;j<keys.size();j++){//loop through keys again
		    	String key2 = keys.get(j);//get second key
		    	ThresholdCombo tc2 = thresCombos.get(key2);//get second combos
		    	
		    	//if combo is dominated flip flag
		    	if(tc2.accuracy>tc.accuracy && tc2.difference<tc.difference && tc2.ji>tc.ji){
		    		flag = false;
		    	}
		    }
		    
		    if(flag){//add non dominated combo
		    	pareto.add(tc);
		    }
		}
		
		System.out.println("\n\nPareto Combos");
		for(ThresholdCombo tc : pareto){
			System.out.println(tc.name+"{ "+"Accuracy: "+tc.accuracy+", Difference: "+tc.difference+", JI: "+tc.ji);
		}
				
		return pareto;
		
	}
	public static void main(String[] args) {
		String imageDir = "C:/Users/nickh/Documents/CellSegmentationDissertation/DissertationPics/images/";
		String saveDir = "C:/Users/nickh/Documents/CellSegmentationDissertation/DissertationPics/Segmented/";
		Test test = new Test(imageDir,saveDir);
		ArrayList<ThresholdCombo> pareto = test.thresholdCombos();
		test.save("C:/Users/nickh/Documents/CellSegmentationDissertation/DissertationPics/pareto.ser",pareto);
		
		
		
	}
}
