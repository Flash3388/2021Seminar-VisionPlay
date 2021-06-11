package frc.team3388;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // connect to network tables. The server is running on the roborio, so
        // we just connect to it.
        NetworkTableInstance networkTableInstance = NetworkTableInstance.getDefault();
        networkTableInstance.startClientTeam(3388);

        NetworkTable table = networkTableInstance.getTable("vision-color");

        NetworkTableEntry minHue = table.getEntry("minHue");
        NetworkTableEntry minSat = table.getEntry("minSat");
        NetworkTableEntry minVal = table.getEntry("minVal");
        NetworkTableEntry maxHue = table.getEntry("maxHue");
        NetworkTableEntry maxSat = table.getEntry("maxSat");
        NetworkTableEntry maxVal = table.getEntry("maxVal");
        NetworkTableEntry distance = table.getEntry("distance");
        minHue.setDouble(0.0);
        minSat.setDouble(44.0);
        minVal.setDouble(103.0);
        maxHue.setDouble(95.0);
        maxSat.setDouble(255.0);
        maxVal.setDouble(255.0);

        // start camera (.close() will be called when the try ends).
        // CvSink allows us to grab images from the camera
        // CvSource allows us to put custom images. We'll put processed images
        try(VideoSource camera = startCamera();
            CvSink sink = new CvSink("vision-sink");
            MjpegServer cameraServer = CameraServer.getInstance()
                    .addSwitchedCamera("camera");
            CvSource postProcess = CameraServer.getInstance()
                    .putVideo("post-process", 320, 480)) {
            // a sink will allow us to grab frames from the camera
            sink.setSource(camera);

            // place the camera in the CameraServer. This will
            // send the image so we can view on the the dashboard.
            cameraServer.setSource(camera);

            // run a loop forever in which we'll perform some image processing.
            // we'll sleep some time between each run to let the camera update image.
            Mat image = new Mat();
            while (true) {
                long frameTime = sink.grabFrame(image);
                if (frameTime == 0) {
                    // ERROR
                    System.err.println(sink.getError());

                    // Sleep for a bit to try and fix the error
                    //noinspection BusyWait
                    Thread.sleep(5);
                    continue;
                }

                // TODO: VISION CODE
                // the image from the camera is in the Mat image
                Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2HSV);

                int mineHueValue = (int) minHue.getDouble(0.0);
                int minSatValue = (int) minSat.getDouble(0.0);
                int minValValue = (int) minVal.getDouble(0.0);
                int maxHueValue = (int) maxHue.getDouble(0.0);
                int maxSatValue = (int) maxSat.getDouble(0.0);
                int maxValValue = (int) maxVal.getDouble(0.0);
                Core.inRange(image,
                        new Scalar(mineHueValue, minSatValue, minValValue),
                        new Scalar(maxHueValue, maxSatValue, maxValValue),
                        image);

                List<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

                Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGB);

                contours.removeIf((contour) -> contour.total() < 10);
                for (int i = 0; i < contours.size(); i++) {
                    MatOfPoint contour = contours.get(i);
                    Imgproc.drawContours(image, contours, i, new Scalar(255, 57, 0), 2);

                    Rect rect = Imgproc.boundingRect(contour);
                    if (1.0 - rect.width / rect.height > Math.abs(0.5)) {
                        continue;
                    }
                    Imgproc.rectangle(image, rect.tl(), rect.br(), new Scalar(0, 57, 255), 3);

                    int xCenter = rect.x + rect.width / 2;
                    int yCenter = rect.y + rect.height / 2;

                    Imgproc.circle(image, new Point(xCenter, yCenter), 3, new Scalar(0, 0, 0), 2);

                    distance.setDouble(xCenter - image.width() / 2);
                }

                // place the post processed image in the second image
                // output in CameraServer. That way we
                // can see what happens.
                postProcess.putFrame(image);

                //noinspection BusyWait
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static UsbCamera startCamera() {
        // create a camera. connected via USB and identified by the given path.
        UsbCamera camera = new UsbCamera("Camera 0", "/dev/video0");
        // set the resolution of it to something reasonable.
        camera.setResolution(320, 480);
        // use JPEG format. MJpeg is the streaming format of jpeg images.
        camera.setPixelFormat(VideoMode.PixelFormat.kMJPEG);
        // set fps. 30 FPS is reasonable.
        camera.setFPS(30);

        return camera;
    }
}
