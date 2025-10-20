// From https://github.com/android/ndk-samples/blob/main/native-midi/app/src/main/java/com/example/nativemidi/AppMidiManager.java
/*
 * License notice of the original file
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Changes by Thomas Braschler and Mathis Braschler: removed midi sending part
 * and updated for suitable interaction with
 * the rest of this project, and added a few more comments
 */

package com.mathis.inputMidi.hardwareMidi.support;

import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.util.Log;

import com.mathis.midiBase.hardwareMidiNativeReceiver;
import com.mathis.midiBase.hardwareMidiNativeSetup;


import java.util.ArrayList;

/**
 * Primary class to handle interaction with physical midi devices. Here restricted to receiving
 * midi messages from a single hardware midi source.
 * This is derived from <a href="https://github.com/android/ndk-samples/blob/main/native-midi/app/src/main/java/com/example/nativemidi/AppMidiManager.java">native-midi example</a>
 * Please check out the original file if you are looking for generic midi functionality, this has been
 * restricted to what is needed in this particular project
 */
public class AppMidiSupport implements hardwareMidiNativeReceiver {


    /**
     * The Android midi manager
     */
    private MidiManager mMidiManager;

    /**
     * Handler for performing the actual interaction with the native layer
     */
    private hardwareMidiNativeSetup mSetupHandler=null;

    /**
     * Selected hardware midi device from which we are receiving midi messages
     */
    private MidiDevice mReceiveDevice; // an "Output" device is one we will RECEIVE data FROM

    /**
     * Configurable object which to which the incoming midi messages will be transmitted
     */
    private hardwareMidiNativeReceiver mMessageReceiver=null;


    /** Constructor
     *
     * @param midiManager Android MidiManger. You can obtain this in an activity via
     *                    (MidiManager) getSystemService(Context.MIDI_SERVICE)
     */
    public AppMidiSupport(MidiManager midiManager) {
        mMidiManager = midiManager;
    }

    /**
     * Scan attached Midi devices forcefully from scratch
     * @param receiveDevices, container for listing the receive devices
     */
    public void ScanMidiDevices(ArrayList<MidiDeviceInfo> receiveDevices) {

        receiveDevices.clear();
        MidiDeviceInfo[] devInfos = mMidiManager.getDevices();
        for(MidiDeviceInfo devInfo : devInfos) {

            String deviceName =
                    devInfo.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
            if (deviceName == null) {
                continue;
            }


            int numOutPorts = devInfo.getOutputPortCount();
            if (numOutPorts > 0) {
                receiveDevices.add(devInfo);
            }
        }
    }

    /** Set the hardwareMidiNativeReceiver object. This object will be notified with
     * midi messages received in the native tier
     * @param receiver hardwareMidiNativeReceiver object
     */
    public void setMessageReceiver(hardwareMidiNativeReceiver receiver) {
        mMessageReceiver=receiver;
    }

    public void setMidiNativeSetupHandler(hardwareMidiNativeSetup setupHandler) {
        mSetupHandler=setupHandler;
    }

    public boolean isMidiNativeSetupHandlerSet() {
        return mSetupHandler!=null;
    }


    /**
     * Local class for callback upon hardware midi device opening
     * The onDeviceOpenend method of this class will be invoked after the midi hardware
     * device has been opened for receiving messages. Here, the now active device is
     * stored as mReceiveDevice, and the native implementation is instructed to start the midi
     * reading thread.
     */
    public class OpenMidiReceiveDeviceListener implements MidiManager.OnDeviceOpenedListener {
        @Override
        public void onDeviceOpened(MidiDevice device) {
            mReceiveDevice = device;
            Log.i("AppMidiSupport","start reading midi");
            startReadingMidi(mReceiveDevice, 0/*mPortNumber*/);
        }
    }


    /**
     * The use has selected a midi device, try to open it for listening to it
     * @param devInfo MidiDeviceInfro object describing the hardware midi device to which we want
     *                to listen
     */
    public void openReceiveDevice(MidiDeviceInfo devInfo) {
        mMidiManager.openDevice(devInfo, new OpenMidiReceiveDeviceListener(), null);
    }

    /**
     *  Unset the mReceiveDevice to indicate it is not active anymore.
     */
    public void closeReceiveDevice() {
        if (mReceiveDevice != null) {
            // Native API
            mReceiveDevice = null;
        }
    }


    /** Possibility to actively load the native implementation
     * not needed if loaded elsewhere
     */
    public static void loadNativeAPI() {
        System.loadLibrary("native_midi");
    }

    /** Init the native tiers
     * This permits callback with natively received midi messages
     */
    public void initNative()
    {
        mSetupHandler.initNative();
    }

    /**
     * Start midi reading thread in the native implementation
     * @param receiveDevice The selected midi receive device
     * @param portNumber Possiblity to set a specific port, by default, provide 0
     */
    public void startReadingMidi(MidiDevice receiveDevice, int portNumber){
        mSetupHandler.startReadingMidi(receiveDevice,portNumber);
    }

    /*
    * Stop the native midi reading thread
     */
    public void stopReadingMidi()
    {
        mSetupHandler.stopReadingMidi();
    }

    /**
     * callback notified when a midi message is received by the native tier
     * @param message
     */
    public void onNativeMessageReceive(final byte[] message) {
        if(mMessageReceiver != null) {
            mMessageReceiver.onNativeMessageReceive(message);
        }

    }


}
