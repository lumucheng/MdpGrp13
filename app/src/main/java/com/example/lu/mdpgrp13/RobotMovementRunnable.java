package com.example.lu.mdpgrp13;

/**
 * Created by Mucheng on 5/10/15.
 */
public class RobotMovementRunnable implements Runnable {

    private char command;
    private PixelGridView pixelGridView;

    public RobotMovementRunnable(PixelGridView pixelGridView, char command) {

        this.command = command;
        this.pixelGridView = pixelGridView;
    }

    public char getCommand() {
        return command;
    }

    public void setCommand(char command) {
        this.command = command;
    }

    @Override
    public void run() {
        if (command == 'r' ||
                command == 'l') {
            pixelGridView.rotateRobot(String.valueOf(command));
        } else {
            pixelGridView.moveRobot(String.valueOf(command));
        }

    }
}
