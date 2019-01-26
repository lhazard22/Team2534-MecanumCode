/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/
package frc.robot;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.drive.MecanumDrive;
import edu.wpi.first.wpilibj.XboxController;
import com.ctre.phoenix.motorcontrol.can.*;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.I2C.Port; 
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.Encoder;

import edu.wpi.cscore.*;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.UsbCamera;




public class Robot extends IterativeRobot {

  MecanumDrive m_robotDrive;
  WPI_TalonSRX frontLeft = new WPI_TalonSRX(RobotMap.talonFrontLeftChannel);
  WPI_TalonSRX rearLeft = new WPI_TalonSRX(RobotMap.talonRearLeftChannel);
  WPI_TalonSRX frontRight = new WPI_TalonSRX(RobotMap.talonFrontRightChannel);
  WPI_TalonSRX rearRight = new WPI_TalonSRX(RobotMap.talonRearRightChannel);
  Encoder testCoder;
  double deadzone = 0.15;
  double JoyY = 0;
  double JoyX = 0;
  double JoyZ = 0;
  boolean JoyA;
  double Target = 0.5;
  double CorrectSpeed = 0.2;
  XboxController controller = new XboxController(RobotMap.xBoxControllerChannel);
  int frames = 30;
  double currentData;

 
  //instantiate output of PIDout
  PIDout output = new PIDout(this); 
  PIDoutX strafeOutput = new PIDoutX(this);
  PIDoutY forwardOutput = new PIDoutY(this);
  
  //Ints. Class 
  CameraSource limelight = new CameraSource(this); 
  CameraSourceX limelightX = new CameraSourceX(this);
  CameraSourceY limelightY = new CameraSourceY(this);

  //Set PIDs
  PIDController visionLoop = new PIDController(0.03, 0.0, 0.0, limelight, output);
  PIDController strafeLoop = new PIDController(0.12, 0.0, 0.0, limelightX, strafeOutput);
  PIDController forwardLoop = new PIDController(0.04, 0.0, 0.0, limelightY, forwardOutput);
  
  //Global Varable for CameraValues
  public double CameraValue = 0;
  public double StrafeValue = 0;
  public double ForwardValue = 0;
  double actualSkew;


  @Override
  public void robotInit() {
    //Setup Drive Train
    frontLeft.setInverted(true);
    rearLeft.setInverted(false);
    frontRight.setInverted(true);
    m_robotDrive = new MecanumDrive(frontLeft, rearLeft, frontRight, rearRight);
    
    //Setup Encoders
    testCoder = new Encoder(RobotMap.encoderAChannel, RobotMap.encoderBChannel, false, Encoder.EncodingType.k4X);
    testCoder.setDistancePerPulse((Math.PI * 8) / 360);     
    //Seting Camera value Ranges and Setpoints
    strafeLoop.setSetpoint(0.0);
    strafeLoop.setOutputRange(-1,1);
    strafeLoop.setInputRange(-25.0, 25.0);
    
    visionLoop.setSetpoint(0.0);
    visionLoop.setOutputRange(-1,1);
    visionLoop.setInputRange(-25.0, 25.0);
    
    forwardLoop.setSetpoint(8.0);
    forwardLoop.setOutputRange(-0.5,0.5);
    forwardLoop.setInputRange(-25.0, 25.0); 

    /*new Thread(() -> {
        
      UsbCamera camera = CameraServer.getInstance().startAutomaticCapture();
      
      camera.setVideoMode(VideoMode.PixelFormat.kMJPEG, 360, 320, frames);

      CvSink cvSink = CameraServer.getInstance().getVideo();
      CvSource outputStream = CameraServer.getInstance().putVideo("Blur", 480, 320);

      Mat source = new Mat();
      Mat output = new Mat();
      
      while(!Thread.interrupted()) {
          cvSink.grabFrame(source);
          currentData = camera.getActualDataRate();
          if (currentData > 3.5 && frames > 8){
            frames = frames - 2;
            camera.setVideoMode(VideoMode.PixelFormat.kMJPEG, 360, 320, frames);
          }else if (currentData < 1.5 && frames < 30){
            frames = frames + 2;
            camera.setVideoMode(VideoMode.PixelFormat.kMJPEG, 360, 320, frames);
          }
          Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2GRAY);
          outputStream.putFrame(output);
      }).start();*/
    }

  @Override
  public void teleopPeriodic() {
    //Gather encoder position, post to smartDashboard. Chech to see if B is pressed to reset encoder.
    if (controller.getRawButton(2)){
      testCoder.reset();
    }
    SmartDashboard.putNumber("Encoder Distance:" , testCoder.getDistance());   
    SmartDashboard.putNumber("Encoder Value:" , testCoder.get());  
    
    
    
    // Use the joystick X axis for lateral movement, Y axis for forward
    // movement, and Z axis for rotation.
    NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
    NetworkTableEntry tx = table.getEntry("tx");
    NetworkTableEntry ty = table.getEntry("ty");
    NetworkTableEntry ta = table.getEntry("ta");
    NetworkTableEntry ts = table.getEntry("ts");

    //read values periodically
    double x = tx.getDouble(0.0);
    double y = ty.getDouble(0.0);
    double area = ta.getDouble(0.0);
    double skew = ts.getDouble(0.0);
    CameraValue = x;
    ForwardValue = area;
    
    if (skew > -45){
      actualSkew = Math.abs(skew);
    }else {
      actualSkew = Math.abs(skew) - 90;
    }
    StrafeValue = actualSkew;
   

    //post to smart dashboard periodically
    SmartDashboard.putNumber("LimelightX", x);
    SmartDashboard.putNumber("LimelightY", y);
    SmartDashboard.putNumber("LimelightArea", area);
    SmartDashboard.putNumber("LimelightSkew", actualSkew);
    

    JoyA = controller.getRawButton(RobotMap.xBoxButtonAChannel);
    JoyY = controller.getRawAxis(RobotMap.xBoxLeftStickYChannel);
    JoyX = controller.getRawAxis(RobotMap.xBoxLeftStickXChannel);
    JoyZ = controller.getRawAxis(RobotMap.xBoxRightStickXChannel);

    //Read Values on Smartdashboard
    SmartDashboard.putNumber("JoyX", JoyX);
    SmartDashboard.putNumber("JoyY", JoyY);
    SmartDashboard.putNumber("JoyZ", JoyZ);
    
    //Deadzone
    if (Math.abs(JoyY) < (deadzone)) {
      JoyY = 0;
    }
    if (Math.abs(JoyX) < (deadzone)) {
      JoyX = 0;
    }
    if (Math.abs(JoyZ) < (deadzone)) {
      JoyZ = 0;
    }


    //Controlling PID Loops 
    if (JoyA){
      visionLoop.enable();
      strafeLoop.enable();
      forwardLoop.enable();
      m_robotDrive.driveCartesian(strafeOutput.outputX, forwardOutput.outputY, output.outputSkew, 0.0);
    } else {
      visionLoop.disable();
      strafeLoop.disable();
      forwardLoop.disable();
      m_robotDrive.driveCartesian(JoyX, -JoyY, -JoyZ, 0.0);
    }

  }
}
