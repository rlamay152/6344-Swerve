// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.List;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.SwerveControllerCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.commands.DefaultDriveCommand;
import frc.robot.subsystems.DrivetrainSubsystem;

public class RobotContainer {
  private final DrivetrainSubsystem m_drivetrainSubsystem = new DrivetrainSubsystem();

  private final XboxController m_controller = new XboxController(0);
  private final JoystickButton zeroGyro = new JoystickButton(m_controller, XboxController.Button.kStart.value);


  public RobotContainer() {
    // Set up the default command for the drivetrain.++
    // The controls are for field-oriented driving:
    // Left stick Y axis -> forward and backwards movement
    // Left stick X axis -> left and right movement
    // Right stick X axis -> rotation
    m_drivetrainSubsystem.setDefaultCommand(new DefaultDriveCommand(
            m_drivetrainSubsystem,
            () -> -modifyAxis(m_controller.getLeftX()) * DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND,
            () -> -modifyAxis(m_controller.getLeftY()) * DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND,
            () -> -modifyAxis(m_controller.getRightX()) * DrivetrainSubsystem.MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND
    ));

    // Configure the button bindings
    configureButtonBindings();
  }

  /**
   * Use this method to define your button->command mappings. Buttons can be created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {
    // Back button zeros the gyroscope
    zeroGyro.onTrue(new InstantCommand(() -> m_drivetrainSubsystem.zeroGyroscope()));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // An ExampleCommand will run in autonomous
    // 1. Create trajectory settings
    TrajectoryConfig trajectoryConfig = new TrajectoryConfig(
      DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND_TRAJECTORY, 
      DrivetrainSubsystem.MAX_ACCELERATION_METERS_PER_SECOND_SQUARED)
        .setKinematics(DrivetrainSubsystem.m_kinematics);

    // 2. Generate trajectory
    Trajectory trajectory = TrajectoryGenerator.generateTrajectory(
      new Pose2d(0, 0, new Rotation2d(0)), 
      List.of(
        new Translation2d(1, 0),
        new Translation2d(-1, 0),
        new Translation2d(0, 1),
        new Translation2d(-1, 0)), 
      new Pose2d(0, -1, Rotation2d.fromDegrees(0)), 
      trajectoryConfig);

      // 3. Define PID controllers for tracking trajectory
      PIDController xController = new PIDController(Constants.kPXController, 0, 0);
      PIDController yController = new PIDController(Constants.kPYController, 0, 0);
      ProfiledPIDController thetaController = new ProfiledPIDController(
        Constants.kPThetaConroller, 0, 0, Constants.kThetaControllerConstraints);
      thetaController.enableContinuousInput(-Math.PI, Math.PI);

      // 4. Construct command to follow trajectory
      SwerveControllerCommand swerveControllerCommand = new SwerveControllerCommand(
        trajectory, 
        m_drivetrainSubsystem::getPose, 
        DrivetrainSubsystem.m_kinematics,
        xController,
        yController,
        thetaController,
        m_drivetrainSubsystem::drive, 
        m_drivetrainSubsystem);

        // 5. Add some init and wrap-up, and return everything
        return new SequentialCommandGroup(
          new InstantCommand(() -> m_drivetrainSubsystem.resetOdometry(trajectory.getInitialPose())),
          swerveControllerCommand,
          new InstantCommand(() -> m_drivetrainSubsystem.stopModules()));



  }

  private static double deadband(double value, double deadband) {
    if (Math.abs(value) > deadband) {
      if (value > 0.0) {
        return (value - deadband) / (1.0 - deadband);
      } else {
        return (value + deadband) / (1.0 - deadband);
      }
    } else {
      return 0.0;
    }
  }

  private static double modifyAxis(double value) {
    // Deadband
    value = deadband(value, 0.05);

    // Square the axis
    value = Math.copySign(value * value, value);

    return value;
  }
}