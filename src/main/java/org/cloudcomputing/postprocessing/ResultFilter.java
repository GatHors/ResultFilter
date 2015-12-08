package org.cloudcomputing.postprocessing;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class ResultFilter {
	static {
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try {
//            //System.setProperty("java.library.path", "/home/gathors/proj/libs");
//            //System.loadLibrary(Core.NATIVE_xxx);
//            //System.loacLibrary("/home/gathors/proj/libs/opencv-300.jar");
            System.load((new File("/home/gathors/proj/v-opencv/FeatureExtraction/libs/libopencv_java2412.so")).getAbsolutePath());
            } catch (UnsatisfiedLinkError e) {
                System.err.println("\nNATIVE LIBRARY failed to load...");
                System.err.println("ERROR:"+e);
                System.err.println("NATIVE_LIBRARY_NAME:"+Core.NATIVE_LIBRARY_NAME);
                System.err.println("#"+System.getProperty("java.library.path"));
                System.exit(1);
            }
	}
	public static void main(String[] args) throws IOException {
		// read username from arguments
		String username = args[0];
		
		//define the path of dataset and output
		String dataset_path = "/home/gathors/pre/";
		String user_path = "/home/gathors/proj/v-opencv/user/";

		
		//test 
	//	String dataset_path = "/home/xiang/testimages/";
	//	String user_path = "/home/xiang/testimages/";

		double hist_ratio = Double.parseDouble(args[1]);
        double threshold = Double.parseDouble(args[2]);
		// read output of map-reduce from tmp directory under specific username
		FileInputStream fstream = new FileInputStream(user_path + username + "/tmp/part-r-00000");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		// create string to store each line of file
		String line;
		// create the map to store the result
		Map<String, Float> map = new HashMap<String, Float>();
		Map<String, Float> refined_map = new HashMap<String, Float>();
		//Read File Line By Line
		while ((line = br.readLine()) != null)   {
			// Print the content on the console
			String[] output = line.split("\\s+");
			String filename = output[0];
			float value = Float.parseFloat(output[1]);
			map.put(filename, value);
//			System.out.println (strLine);
		}

		//Close the input stream
		br.close();
		
		//sort the nodes in descending order by their feature matching value
		List<Map.Entry<String, Float>> list = new ArrayList<Map.Entry<String, Float>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
				public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {
//					return o2.getValue().compareTo(o1.getValue());
					return o2.getKey().compareTo(o1.getKey());
				}
		});

		// define the parameter of HSV histogram
		int hBins = 50;
	   	int sBins = 60;
		MatOfInt histSize = new MatOfInt(hBins, sBins);
		MatOfFloat ranges = new MatOfFloat(0f, 180f, 0f, 256f);
		MatOfInt channels = new MatOfInt(0, 1);
		
		// process the first entry of list first
		Map.Entry<String, Float> head = list.get(0);
		// output to check whether sorted
//		System.out.println(head.getKey() + "      " + head.getValue());
		
		Mat first = Highgui.imread(dataset_path + head.getKey());
		Imgproc.cvtColor(first, first, Imgproc.COLOR_BGR2HSV);
		Mat hist_head = new Mat();
		ArrayList<Mat> list_head = new ArrayList<Mat> ();
		list_head.add(first);
		Imgproc.calcHist(list_head, channels, new Mat(), hist_head, histSize, ranges);
		Core.normalize(hist_head, hist_head, 0, 1, Core.NORM_MINMAX);		
		list.remove(0);
		refined_map.put(head.getKey(), head.getValue());
		
		// iterate the map to process each result image
		for (Map.Entry<String, Float> mapping : list) {
			// output to check whether sorted
//			System.out.println(mapping.getKey() + "      " + mapping.getValue());
			Mat next = Highgui.imread(dataset_path + mapping.getKey());
			Imgproc.cvtColor(next, next, Imgproc.COLOR_BGR2HSV);
			Mat hist_next = new Mat();
			ArrayList<Mat> list_next = new ArrayList<Mat> ();
			list_next.add(next);
			Imgproc.calcHist(list_next, channels, new Mat(), hist_next, histSize, ranges);
			Core.normalize(hist_next, hist_next, 0, 1, Core.NORM_MINMAX);
			double result = Imgproc.compareHist(hist_head, hist_next, 0);
			// third parameter 0 corresponding to method Correlation (method=CV_COMP_CORREL)
			//d(H_1,H_2) = (sum_I(H_1(I) - H_1")(H_2(I) - H_2"))/(sqrt(sum_I(H_1(I) - H_1")^2 sum_I(H_2(I) - H_2")^2))
			//
			//	where
			//
			//	H_k" = 1/(N) sum _J H_k(J)
			//
			//	and N is a total number of histogram bins.
			if (result < hist_ratio) { 
				// two images are determined to be different
				head = mapping;
				hist_head = hist_next;
				refined_map.put(mapping.getKey(), mapping.getValue());
			} else {
				// two images are determined to be similar
				if (mapping.getValue() > head.getValue()) {
					// if mapping's sift factor is higher than head's factor, replace mapping with head)
					refined_map.put(mapping.getKey(), mapping.getValue());
					refined_map.remove(head.getKey());
					hist_head = hist_next;
					head = mapping;
				} else {
					// this image is worse than head, do not replace
				}
			}
			
			
		}
		
		// sort the refined result by its value in descending order
		List<Map.Entry<String, Float>> final_list = new ArrayList<Map.Entry<String, Float>>(refined_map.entrySet());
		Collections.sort(final_list, new Comparator<Map.Entry<String, Float>>() {
				public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {
					return o2.getValue().compareTo(o1.getValue());
//					return o2.getKey().compareTo(o1.getKey());
				}
		});
		
		File file = new File(user_path + username + "/tmp/output");

		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		writeoutput(final_list, file, threshold);
		
	}
	private static void writeoutput(List<Map.Entry<String, Float>> final_list, File file, double threshold) throws IOException {
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		for (Map.Entry<String, Float> entry : final_list) {
//			System.out.println(entry.getKey());
			if (entry.getValue() > threshold) {
                bw.write(entry.getKey() + "\n");
            }
		}
		bw.close();
	}
	
	

}
