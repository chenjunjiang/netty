package com.chenjj.io.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @Author: chenjj
 * @Date: 2018-01-23
 * @Description:
 */
public class TimeServerHandler implements Runnable {

  private Socket socket;

  public TimeServerHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    BufferedReader reader = null;
    PrintWriter writer = null;
    try {
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);
      String currentTime = null;
      String body = null;
      while (true) {
        body = reader.readLine();
        if (body == null) {
          System.out.println("no receive order......");
          break;
        }
        System.out.println("The time server receive order: " + body);
        currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? System.currentTimeMillis() + ""
            : "BAD ORDER";
        writer.println(currentTime);
      }
    } catch (Exception e) {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
      if (writer != null) {
        writer.close();
      }
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
  }
}
