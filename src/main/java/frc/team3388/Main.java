package frc.team3388;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.opencv.core.Mat;

public class Main {

    public static void main(String[] args) {
        // connect to network tables. The server is running on the roborio, so
        // we just connect to it.
        NetworkTableInstance networkTableInstance = NetworkTableInstance.getDefault();
        networkTableInstance.startClientTeam(3388);

        // start camera (.close() will be called when the try ends).
        // CvSink allows us to grab images from the camera
        // CvSource allows us to put custom images. We'll put processed images
        try(VideoSource camera = startCamera();
            CvSink sink = new CvSink("vision-sink");
            CvSource postProcess = CameraServer.getInstance()
                    .putVideo("post-process", 320, 480)) {
            // a sink will allow us to grab frames from the camera
            sink.setSource(camera);

            // place the camera in the CameraServer. This will
            // send the image so we can view on the the dashboard.
            CameraServer.getInstance().addServer(sink);

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

                // place the post processed image in the second image
                // output in CameraServer. That way we
                // can see what happens.
                // postProcess.putFrame(image);

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
