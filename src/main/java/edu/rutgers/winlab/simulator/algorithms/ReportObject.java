/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.algorithms;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

/**
 *
 * @author ubuntu
 */
public class ReportObject {

    private boolean _running;
    public PrintStream writer;

    private final HashMap<String, Integer> _keys = new HashMap<>();
    private final HashMap<Integer, Integer> _counts = new HashMap<>();
    private final HashMap<String, Supplier<String>> _funcKeys = new HashMap<>();
    private final Timer _timer = new Timer("ReportObject");
    private final TimerTask _timerTask = new TimerTask() {
        @Override
        public void run() {
            writeContent(writer);
        }
    };

    public ReportObject() {
        _running = false;
        writer = System.out;
    }

    public void setKey(String name, int key) {
        if (_keys.containsKey(name)) {
            throw new IllegalArgumentException(String.format("Name %s already exists", name));
        }
        if (_counts.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Key %d already exists", key));
        }

        _keys.put(name, key);
        _counts.put(key, 0);
    }

    public void setKey(String name, Supplier<String> func) {
        if (_keys.containsKey(name)) {
            throw new IllegalArgumentException(String.format("Name %s already exists", name));
        }
        _funcKeys.put(name, func);
    }

    public int getCount(int key) {
        return _counts.get(key);
    }

    public void setCount(int key, int value) {
        _counts.put(key, value);
    }

    public int accumulateCountAndGet(int key, int inc) {
        int val = _counts.get(key);
        val += inc;
        _counts.put(key, val);
        return val;
    }

    public synchronized void beginReport() {
        if (_running) {
            return;
        }
        _running = true;
        _timer.scheduleAtFixedRate(_timerTask, 0, 1000);
    }

    public synchronized void endReport() {
        if (!_running) {
            return;
        }
        _running = false;
        _timer.cancel();
        writeContent(writer);
        writer.println();
    }

    public void writeContent(PrintStream writer) {
        writer.print("\r");
        _keys.entrySet().forEach((entry) -> {
            writer.printf("%s=%,d,", entry.getKey(), _counts.get(entry.getValue()));
        });
        _funcKeys.entrySet().forEach((entry) -> {
            writer.printf("%s=%s,", entry.getKey(), entry.getValue().get());
        });
        writer.print("                  \r");
    }

}
