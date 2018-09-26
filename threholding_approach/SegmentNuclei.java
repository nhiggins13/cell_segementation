import java.io.File;
import java.util.ArrayList;

import fiji.threshold.Auto_Threshold;
import fiji.threshold.Auto_Local_Threshold;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ImageConverter;
import sc.fiji.colourDeconvolution.Colour_Deconvolution;
import ij.process.BinaryProcessor;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.measure.ResultsTable;
import ij.io.FileSaver;

public class SegmentNuclei {

	/**
	 * 
	 * @param imagePath
	 * @param dye
	 * @return processed image
	 * 
	 * Hides overlays, color deconvolution, greyscales the image
	 */
	public ImagePlus preprocess(String imagePath, String dye){
		// Open image
		ImagePlus image = IJ.openImage(imagePath);
		
		
		//hide overlay
		if(imagePath.contains(".tif")){
			IJ.run(image,"Hide Overlay", "");
		}
	    
	    //Deconvolution
	    Colour_Deconvolution deconv = new Colour_Deconvolution();
	    image = deconv.computeFirstStain(image,dye);
	    
	    //convert to 8 bit greyscale
	    ImageConverter ic = new ImageConverter(image);
	    ic.convertToGray8();
	    image.updateImage();
	    return image;
	}
	
	/**
	 * 
	 * @param image
	 * @param method
	 * @return image after threshold
	 * 
	 * performs the global threshold
	 */
	public ImagePlus firstThreshold(ImagePlus image, String method){
		//get result table and roi manger
		ResultsTable rt = ResultsTable.getResultsTable();
	 	RoiManager roiMng = RoiManager.getInstance();
	 	
		//reset roi manager and result table
	 	if(roiMng != null){
		    roiMng.reset();
		    rt.reset();
	 	}
		
		
		//Threshold
	    Auto_Threshold thres = new Auto_Threshold();
	    Object[] t = thres.exec(image, method, false, false, true, false, false, false);
	    image = (ImagePlus) t[1];
	    
	    //reduce noise
	    IJ.run(image,"Despeckle","");
	    IJ.run(image,"Fill Holes","");
	    
	    //set measurements and analyze image objects
	    IJ.run(image,"Set Measurements...", "area perimeter redirect=None decimal=3");
	    IJ.run(image,"Analyze Particles...", "size=100-Infinity add");
	    
	    return image;
	}

	/**
	 * 
	 * @param image
	 * @param areaMax
	 * @param perimeterMax
	 * @param roiNormName
	 * @param roiAbName
	 * @return array of paths to roi files
	 * 
	 * identifies rois of an abnormal size
	 */
	public String [] findAbnormal(ImagePlus image,int areaMax, int perimeterMax,String roiNormName,String roiAbName){
		 	
			//get result table and roi manager
			ResultsTable rt = ResultsTable.getResultsTable();
		 	RoiManager roiMng = RoiManager.getInstance();
		 	
		    //lists of the selected and non selected rois
		    ArrayList<Integer> selected = new ArrayList<Integer>();
		    ArrayList<Integer> notSelected = new ArrayList<Integer>();
		    
		    
		    
		    //loop through results and record indexes
		    for(int i =0;i < roiMng.getCount();i++){
		    	
		    	//roi is of abnormal size
		    	if(rt.getValue("Area", i) >areaMax || rt.getValue("Perim.", i) > perimeterMax){
		    		//add roi array filled with overthres hold rois
		    		selected.add(i);
		    		
		    	}
		    	//rois is normal
		    	else{
		    		notSelected.add(i);
		    	}
		    }
	
		    //convert to int array
		    int[] overThres = selected.stream().mapToInt(i -> i).toArray();
		    int[] underThres = notSelected.stream().mapToInt(i -> i).toArray();
		   
		    
		    //save abnormal rois
		    roiMng.setSelectedIndexes(overThres);
		    roiMng.runCommand("Save selected", "./"+roiAbName+".zip");
		    
		    
		    String[] paths;
		    
		    //check array isn't empty
		    if(notSelected.size()>0){
		    	//save normal rois
			    roiMng.setSelectedIndexes(underThres);
			    roiMng.runCommand("Save selected", "./"+roiNormName+".zip");
			    
			    //get save paths
			    String[] paths1 =  {"./"+roiNormName+".zip","./"+roiAbName+".zip"};
			    paths = paths1;
		    }
		    else{
		    	//get save paths
		    	String[] paths2 =  {null,"./"+roiAbName+".zip"};
		    	paths = paths2;
		    }
		    return paths;
		    
	}
	/**
	 * 
	 * @param image
	 * @param abRoiPath
	 * @param thresholdMethod
	 * @return image after threshold 
	 * 
	 * Performs loack threshold on given image
	 */
	public ImagePlus localThreshold(ImagePlus image,String abRoiPath,String thresholdMethod){
		//get result table and roi manger
		ResultsTable rt = ResultsTable.getResultsTable();
	 	RoiManager roiMng = RoiManager.getInstance();
	 	
		//reset roi manager and result table
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	    rt.reset();
	    
	    //open abnormal rois and combine
	    roiMng.runCommand("Open", abRoiPath);
	    roiMng.runCommand("Select All");
	    roiMng.runCommand(image,"Combine");
	    
	    IJ.run(image,"Colors...", "background=white");
	    //clear area outside rois
	    IJ.run(image, "Clear Outside", "");
	   
	    //perform local threshold
	    Auto_Local_Threshold locThres = new Auto_Local_Threshold();
	    Object[] locT = locThres.exec(image, thresholdMethod, 15, 0, 0, true);
	    image = (ImagePlus) locT[0];
	  
	    //reduce noise
	    IJ.run(image,"Despeckle","");
	    IJ.run(image,"Fill Holes","");
	    IJ.run(image, "Clear Outside", "");
	    
	    //reset roi manager and result table
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	    roiMng.runCommand(image,"Show All");
	    rt.reset();
	    
	    
	    //set measurements and analyze image objects
	    IJ.run(image,"Set Measurements...", "area perimeter redirect=None decimal=3");
	    IJ.run(image,"Analyze Particles...", "size=100-Infinity add");
	    
	    
		return image;
	}
	/**
	 * 
	 * @param image
	 * @param roiPath
	 * @param roiName
	 * @return path to roi file
	 * 
	 * Performs watershed method on given rois
	 */
	public String segmentRemaining(ImagePlus image, String roiPath, String roiName){
		//get result table and roi manager
		ResultsTable rt = ResultsTable.getResultsTable();
	 	RoiManager roiMng = RoiManager.getInstance();
	 	
	 	//clear roi manager
		roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	    
	    //open abnormal rois and combine
	    roiMng.runCommand("Open", roiPath);
	    roiMng.runCommand("Select All");
	    roiMng.runCommand(image,"Combine");
	    
	    //clear area outside rois
	    IJ.run(image, "Clear Outside", "");
	    
	    //convert image to mask
	    IJ.run(image, "Convert to Mask", "");
	    
	    //run watershed method 
	    IJ.run(image,"Watershed", "");
	    
	    //reset roi manager and result table
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	    rt.reset();
	    
	    //analyze particles
	    IJ.run(image,"Analyze Particles...", "size=100-Infinity add");
	    
	    //save rois
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Save selected", "./"+roiName+".zip");
	    
	    //return path to roi file
	    return "./"+roiName+".zip";
	    
	}
	
	/**
	 * 
	 * @param path1
	 * @param path2
	 * @param path3
	 * opens seperate roi files and combines them into one file
	 */
	public void combineRois(String path1,String path2,String path3){
		//get roi manager
	 	RoiManager roiMng = RoiManager.getInstance();
	 	
	 	//clear manager
		roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	    
	    //open first set of rois
	    roiMng.runCommand("Open", path1);
	    
	    //check roi path is not blank
	    if(path2 != null){
	    	//open set of rois
	    	roiMng.runCommand("Open", path2);
	    }
	    
	    //open set of rois
	    roiMng.runCommand("Open", path3);
	    
	    //save all rois to one file
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Save selected", "./all.zip");
	    
	    //reset manager
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	}

	/**
	 * 
	 * @param imagePath
	 * @param savePath
	 * @param dye
	 * @param thresholdMethod
	 * @param localThresholdMethod
	 * @param showSegmentation
	 * 
	 * Segments a image into it's nuclei
	 */
	public void createSegmentedImage(String imagePath,String savePath,String dye,String thresholdMethod,String localThresholdMethod, Boolean showSegmentation){
		
	 	
		ImagePlus image = preprocess(imagePath,dye);
		ImagePlus imDup = image.duplicate();
		
		//perform first threshold
		image = firstThreshold(image,thresholdMethod);
		
		//find abnormal rois
		String[] paths = findAbnormal(image,3890,226,"normal1","ab1");
		
		//perform local threshold
		imDup = localThreshold(imDup,paths[1],localThresholdMethod);
		
		//find abnormal rois
		String[] paths2 = findAbnormal(imDup,1800,226,"normal2","ab2");
		
		//perform watershed
		String finalRoiPath = segmentRemaining(imDup,paths2[1],"final");
		
		//combine rois
		combineRois(paths[0],paths2[0],finalRoiPath);
		
	    image = IJ.openImage(imagePath);
	   
	    //hide overlay
  		if(imagePath.contains(".tif")){
  			image.setHideOverlay(true);
  			String path = imagePath.replace("tif", "png");
  			path = path.replaceAll("images", "png");
  			FileSaver fs = new FileSaver(image);
  			fs.saveAsPng(path);
  			image = IJ.openImage(path);
  		}
  		
  		//get roi manager
	 	RoiManager roiMng = RoiManager.getInstance();
	 	roiMng.deselect();
  		
	   
	    
	    
	    
	 	
	 	//open all rois
	 	roiMng.runCommand("Open","./all.zip");
	 	
	 	
	    //show rois on image
	    roiMng.runCommand(image,"Show All");
	    
	    //show image if wanted
	    if(showSegmentation){
	    	image.show();
	    }
	    
	    //save file and print number of nuclei
	    FileSaver fs = new FileSaver(image);
	    String imageName = image.getTitle();
	    fs.saveAsTiff(savePath);
	    	    
	    //reset roi manager and result table
	    ResultsTable rt = ResultsTable.getResultsTable();
	    roiMng.runCommand("Select All");
	    roiMng.runCommand("Delete");
	    rt.reset();
	   
		
		
	}
	
	public static void main(String[] args) {
		String imagePath = "C:/Users/nickh/Documents/CellSegmentationDissertation/DissertationPics/images/h400_20_h001-overlay_nuclei.tif";//./test.tif";
		
		String savePath = "./segmented.tif";
		
		SegmentNuclei sn = new SegmentNuclei();
		//segment image
		sn.createSegmentedImage(imagePath, savePath, "H&E", "Minimum", "Bernsen", true);
	}
	
}
