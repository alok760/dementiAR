package neurobreach.dementiAR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.neurobreach.dementiAR.R;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

public class Recognize extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 0);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    public static final int SEARCHING= 1;
    public static final int IDLE= 2;

    private static final int frontCam =1;
    private static final int backCam =2;
static  String labelu="";

    private int faceState=IDLE;


    private Mat                    mRgba;
    private Mat                    mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private int mLikely=999;

    String mPath="";

    private Tutorial3View   mOpenCvCameraView;

    private ImageView Iv;
    Bitmap mBitmap;
    Handler mHandler;
      Timer autoScan;
      static String nameFound;

      Button showDetails;
      TextView details;

    PersonRecognizer fr;
    ToggleButton scan;

    Set<String> uniqueNames = new HashSet<String>();

    // max number of people to detect in a session
    String[] uniqueNamesArray = new String[10];

    static final long MAXIMG = 10;

    Labels labelsFile;
    static {
        OpenCVLoader.initDebug();
        System.loadLibrary("opencv_java");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    fr=new PersonRecognizer(mPath);
                    String s = getResources().getString(R.string.Straininig);
                    //Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
                    fr.load();
                    if(!fr.canPredict()) {
                        //scan.setChecked(false);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.SCanntoPredic), Toast.LENGTH_LONG).show();
                        Intent i=new Intent(Recognize.this,NameActivity.class);
                        startActivity(i);
                        //return;
                    }
                    faceState=SEARCHING;

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;


            }
        }
    };

    public Recognize() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        //scan = (ToggleButton) findViewById(R.id.scan);
        //final TextView results = (TextView) findViewById(R.id.results);

        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //mPath=getFilesDir()+"/facerecogOCV/";
        mPath = Environment.getExternalStorageDirectory()+"/facerecogOCV/";

        Log.e("Path", mPath);

        showDetails= (Button) findViewById(R.id.showdetails);
        details=(TextView)findViewById(R.id.details_text);

        labelsFile= new Labels(mPath);




        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                /*
                    display a newline separated list of individual names
                 */
                String tempName = msg.obj.toString();
                if (!(tempName.equals("Unknown"))) {
                    tempName = capitalize(tempName);
                    uniqueNames.add(tempName);
                    uniqueNamesArray = uniqueNames.toArray(new String[uniqueNames.size()]);
                    StringBuilder strBuilder = new StringBuilder();
                    for (int i = 0; i < uniqueNamesArray.length; i++) {
                        strBuilder.append(uniqueNamesArray[i] + "\n");
                    }
                    String textToDisplay = strBuilder.toString();
                    //results.setText(textToDisplay);
                }
            }
        };


        boolean success=(new File(mPath)).mkdirs();
        if (!success)
        {
            Log.e("Error","Error creating directory");
        }

        Button addnew = (Button) findViewById(R.id.addnew);
        addnew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(Recognize.this,NameActivity.class);
                startActivity(i);

            }
        });

        showDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                details.setVisibility(View.VISIBLE);

                SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                //int defaultValue = getResources().getInteger(R.integer.saved_high_score_default_key);
                String  relationRec=prefs.getString(labelu,"No Details Found");
                details.setText(relationRec);
                submitData();

                //int highScore = sharedPref.getInt(getString(R.string.saved_high_score_key), defaultValue);
                //name on rectangle

            }
        });

    }

    private void submitData() {

        StringRequest stringRequest=new StringRequest(Request.Method.GET,
                "http://alok760.ml/dementia/write.php?name='"+labelu+"'&details='Bhadwasor'&relation='chutiya'&lattitude='0.000'&longitude='1.1111'", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("Response ",response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

               // loading.dismiss();

                //Log.d("LoginActivity",error.toString());

                Toast.makeText(Recognize.this
                        ,"Error Connecting to Server."
                        ,Toast.LENGTH_SHORT).show();

            }
        });



        RequestQueue queue= Volley.newRequestQueue(Recognize.this);
        queue.add(stringRequest);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            //  mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            /*if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);*/
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();

        if ((facesArray.length>0) && (faceState==SEARCHING))
        {
            Mat m=new Mat();
            m=mGray.submat(facesArray[0]);
            mBitmap = Bitmap.createBitmap(m.width(),m.height(), Bitmap.Config.ARGB_8888);


            Utils.matToBitmap(m, mBitmap);
            Message msg = new Message();
            String textTochange = "IMG";
            msg.obj = textTochange;
            //mHandler.sendMessage(msg);


            textTochange = fr.predict(m);
            mLikely=fr.getProb();
            labelu=textTochange;
            msg = new Message();
            msg.obj = textTochange;
            mHandler.sendMessage(msg);

        }
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        if(mLikely!=-1){
        Core.putText (
                mRgba,                          // Matrix obj of the image
                ""+labelu,          // Text to be added
                new Point(100, 150),               // point
                Core.FONT_HERSHEY_SIMPLEX ,      // front face
                3,                               // front scale
                new Scalar(0, 255, 0, 0),             // Scalar object for color
                4                                // Thickness
        );


        }


        return mRgba;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

//    because capitalize is the new black
    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }
}
