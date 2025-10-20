/* This file is based on code at
 * https://github.com/googlearchive/android-MidiSynth/blob/master/Application/src/main/java/com/example/android/midisynth/MidiSynthDeviceService.java
 * under an Apache license:
 *
 * Copyright (C) 2015 The Android Open Source Project
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
 * Changes by Thomas Braschler and Mathis Braschler, under GPL v3.0
 * These changes concern adaptation to fit within the remainder of the project, and also the
 *  use as a static class. The role is a bit different here than in the example on github:
 * Rather than directly driving the synthesizer (mSynthEngine on the github file), the class
 * now manages the selection of a software midi port and directs the internal
 * SynthDeviceServiceMidiReceiver object, which receives the software midi messages and transmits
 * them to secondary receivers elsewhere in the application.
 */

package com.mathis.inputMidi.softwareMidi;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiReceiver;
import android.util.Log;

/**
 * Class used to open a virtual (aka, software) midi port on which the application can
 * receive midi messages from other applications. A part from also using midi messages, this
 * mechanism is entirely distinct and separate from hardware midi message reception. For the
 * hardware part, see the hardwareMidiManager class
 */

public class MidiSynthDeviceService extends MidiDeviceService {


    /**
     * Is the software midi message reception currently running?
     */
    public boolean mSynthDeviceServiceMidiReceiverStarted = false;
    /** Singleton class instance */
    private static MidiSynthDeviceService mInstance;

    /**
     * Delegate object that will receive the actual midi messages
     */
    private static final SynthDeviceServiceMidiReceiver mSynthDeviceServiceMidiReceiver = new SynthDeviceServiceMidiReceiver();

    /**
     * Do we currently transmit the received messages to the application receivers (or just receive and
     * keep them silent). The application receiver are meant to be objects implementing the softwareMidiReceiver
     * interface, configured via the setSoftwareMidiMessageReceiver receiver function.
     * @param doReceive True if transmission to the receiver is desired, false otherwise
     */
    public static void setTransmitSoftwareMidiMessagesToReceiver(boolean doReceive)
    {
        mSynthDeviceServiceMidiReceiver.setTransmitSoftwareMidiMessagesToReceiver(doReceive);
    }

    /**
     * Are we currently retransmitting potential incoming software midi messages to the receives
     * @return True if we are retransmitting, false otherwise
     */

    public static boolean isTransmittingSoftwareMidiMessagesToReceiver()
    {

        return mSynthDeviceServiceMidiReceiver.getTransmitSoftwareMidiMessagesToReceiver();
    }

    /**
     * Configure a receiver object to which the incoming software midi messages will be retransmitted
     * @param rec The target receiver, which needs to implement the softwareMidiReceiver interface
     */

    public static void setSoftwareMidiMessageReceiver(softwareMidiReceiver rec)
    {
        Log.i("MidiSynthDeviceService","Set softwareMidiReceiver");
       mSynthDeviceServiceMidiReceiver.setSoftwareMidiMessageReceiver(rec);
    }


    /** Keep track of the singleton instance */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MidiSynthDeviceService","onCreate");
        mInstance = this;

    }


    /**
     * Create the message receivers for each port
     * @return List of message recievers. For now, just one receiver in the list
     */

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        Log.i("MidiSynthDeviceService","onGetInputPortReceivers");
        return new MidiReceiver[] {mSynthDeviceServiceMidiReceiver};
    }



    /** Callback function when software midi ports are added or removed
     * This will get called when clients connect or disconnect.
     */
    @Override
    public void onDeviceStatusChanged(MidiDeviceStatus status) {
        Log.i("MidiSynthDeviceService","onDeviceStatusChanged: active");
        if (status.isInputPortOpen(0) && !mSynthDeviceServiceMidiReceiverStarted) {
            //M_SYNTH_DEVICE_SERVICE_MIDI_RECEIVER.start();

            mSynthDeviceServiceMidiReceiverStarted = true;


        } else if (!status.isInputPortOpen(0) && mSynthDeviceServiceMidiReceiverStarted){
            //M_SYNTH_DEVICE_SERVICE_MIDI_RECEIVER.stop();
            mSynthDeviceServiceMidiReceiverStarted = false;
            Log.i("MidiSynthDeviceService","onDeviceStatusChanged: inactive");
        }
        mSynthDeviceServiceMidiReceiver.onDeviceStatusChanged(mSynthDeviceServiceMidiReceiverStarted);
    }

    public static void checkDeviceStatus()
    {
        if( mInstance != null) {
            mSynthDeviceServiceMidiReceiver.onDeviceStatusChanged(mInstance.mSynthDeviceServiceMidiReceiverStarted);
        }
    }




}