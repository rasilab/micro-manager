package org;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow; 

public class TensorflowTest {


    SavedModelBundle smb_;
    private Session sess_;
    private String modelPath_;
    private String modelName_; //TODO get the short name
    
    public static void main(String[] args){
        TensorflowTest tft = new TensorflowTest("C:\\projects\\micro-manager2Viewer3D\\TFTest\\mmtestgraph");
        float[] test = new float[2048*2048];
        Arrays.fill(test, 2.0f);
        double output = -1.0;
        try {
          output = tft.runModel(test);
        } catch (Exception ex) {
           System.out.println(ex.getMessage());
        }
        System.out.println("the value is :" + output);
    }
    
    public TensorflowTest(String s) {
        //Try to load a model if one is remembered
        modelPath_ = s;
        try {
           if (modelPath_ != null) {        
              SavedModelBundle b = SavedModelBundle.load(modelPath_,"serve");
              sess_ = b.session();
              //String sep = "\\\\";
              //modelName_ = modelPath_.split(sep)[modelPath_.split(sep).length-1];
              modelName_ = modelPath_;
           }
        } catch (Exception e) {
           //problem loading autofocus model
           
        }
    }

    public double runModel(float[] input) throws Exception {
       
        long[] shape = new long[]{1,2048,2048};
        Tensor inputTensor = Tensor.create(shape, FloatBuffer.wrap(input));
         
       // long start = System.currentTimeMillis();
       List<Tensor<?>> run = sess_.runner().feed("input", inputTensor).fetch("Mean").run();
       Tensor<Float> result = run.get(0).expect(Float.class);
//       System.out.println("Time to evaluate:" + (System.currentTimeMillis() - start) );
       
       float[] res=new float[1];
       result.copyTo(res); 
       double model_output = res[0];
    //   System.out.println("Total prediction: " + predictedDefocus);
       
      // Generally, there may be multiple output tensors, all of them must be closed to prevent resource leaks.
      inputTensor.close();
      result.close();
      
       return model_output;
    }


    public String getModelName() {
       if (modelPath_ == null ) {
          return "No model loaded";
       } else {
          return modelName_;
       }
    }

}