// ----------------------------------------------------------------------------
//
//  Copyright (C) 2025 Thomas and Mathis Braschler <thomas.braschler@gmail.com>
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// Note that this file is largely inspired by the extensive description
// of Android's native midi interface available at:
// https://developer.android.com/ndk/guides/audio/midi
// To be even more precise, the basis of this file is the github example referenced
// in the description:
// https://github.com/android/ndk-samples/blob/main/native-midi/app/src/main/java/com/example/nativemidi/AppMidiManager.java
// The ndk example files are under an apache licence (available at
// https://github.com/android/ndk-samples/blob/main/LICENSE)
// There are a number of changes here from the original AppMidiSupport.java file:
// - First, the class is renamed to hardwareMidiManager. It indeed does not cover
//   handling of software midi messages (which are on a virtual, rather than physical port)
// - Second, only the receiving part was retained (only incoming messages from the hardware port)
//   not the sending part
// ----------------------------------------------------------------------------
package com.mathis.inputMidi.hardwareMidi;

import static android.media.midi.MidiManager.TRANSPORT_MIDI_BYTE_STREAM;

import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;

import com.mathis.midiBase.hardwareMidiNativeReceiver;
import com.mathis.midiBase.hardwareMidiNativeSetup;
import com.mathis.inputMidi.hardwareMidi.support.AppMidiSupport;


import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
Static interface class hardwareMidiManager. Do not instantiate. This class handles the listing of the available
 midi hardware devices (for reception of data), and the selection of a particular midi hardware device to actually
 receive midi data. For this second part, this class internally relies on AppMidiSupport.<br />
 To use the hardwareMidiManager class , you need to : <br />
 1) call hardwareMidiManager.setHardwareMidiManager with a MidiManager object obtained from Android <br />
 2) call hardwareMidiManager.initNative() to set up the JNI callback mechanisms <br />
 3) call hardwareMidiManager.setHardwareMidiDeviceCallback to receive callbacks when an actual device is
     attached or detached <br />
 4) call hardwareMidiManager.setMessageReceiver to indicate the object that
   should be notified of the incoming midi messages<br />
 5) possibly use hardwareMidiManager.scanhardwareMidiDevices to populate a list of
     device choices<br />
 6) Once the user or the program has chosen a device to use for input, open it
     via hardwareMidiManager.hardwareMidiOpenReceiveDevice<br /><br />
   The last action causes a Linux thread to be started via a JNI call to
   AppMidiSupport.cpp, jni function startReadingMidi
   If messages are received from the hardware device, they are returned
   from the native functions to AppMidiSupport<br /><br />
   For an example of implementation of this sequence, see baseAeolusMidiActivity. Steps 1-4 are implemented in
   initMidiAeolus, step 5 is implemented in ScanMidiDevices (which is invoked by initMidiAeolus), and step 6
   is implemented with a default action of selecting the first avaible device in the onDeviceListChange()
   routine, which is called initially by ScanMidiDevices, and also as an event when the midi device list changes.
 */
public class hardwareMidiManager {
    /**
     * Singleton of the calss
     */
    private static AppMidiSupport mAppMidiSupport;

    /**
     * Reference to the android midimanager
     */
    private static MidiManager mMidiManager;

    /**
     * The available hardware midi devices, from which data can be received. Actual reception
     * is only from one device at the time in the present implementation
     */
    private static ArrayList<MidiDeviceInfo> mReceiveDevices = new ArrayList<MidiDeviceInfo>();

    /**
     * Get the list of available midi devices for receiving data from them
     * @return List of midi-devices from which data can be receivedc
     */
    public static ArrayList<MidiDeviceInfo> availableReceiveDevices(){
        return mReceiveDevices;
    }

    /**
     * Indicate the global android midi manager; typically,
     * one can obtain this in a onCreate function of an Activity
     * via a call of the type (MidiManager) getSystemService(Context.MIDI_SERVICE)
     * The Android midi Manager handles hardware connection, but
     * not software midi ports
     * @param midiManager
     */
    public static void setHardwareMidiManager(MidiManager midiManager)
    {
        mMidiManager = midiManager;
        mAppMidiSupport = new AppMidiSupport(mMidiManager);

    }

    /** Set device notification for new or lost connections of
     * hardware midi devices
     * @param theCallback Object that will receive the callback when devices are added or removed. This
     *                    object must implement the MidiManager.DeviceCallback interface. An simple example
     *                    of a suitable class for this is provided as baseAeolusMidiActivity.MidiDeviceCallback
     */

    public static void setHardwareMidiDeviceCallback(MidiManager.DeviceCallback theCallback)
    {
        mMidiManager.registerDeviceCallback(TRANSPORT_MIDI_BYTE_STREAM, Executors.newSingleThreadExecutor(),theCallback);

    }

    /**
     * A hardware midi device
     * has been selected for input by the user and we should start listening to it.
     * Calling this function will open the the selected hardware midi device for receiving midimessages
     * within the native code; technically, this happens through the corresponding method invocations
     * on the mAppMidiSupport object.
     * @param device The midi hardware device to be opened.
     */

    public static void hardwareMidiOpenReceiveDevice(MidiDeviceInfo device)
    {
        mAppMidiSupport.openReceiveDevice(device);
    }

    /**
     * Scan the hardware midi devices for devices from which we can receive midi data. Populate
     * the list of midi devices (mReceiveDevices) with this data.
     */

    public static void scanhardwareMidiDevices()
    {
        mAppMidiSupport.ScanMidiDevices(mReceiveDevices);
    }


    /** Set the hardware midi message receiver
     * The hardware midi message receiver will be called back with the midi messages
     * @param receiver receiver implementing the interface hardwareMidiNativeReceiver for receiving midi
     *                 messages
     */
    public static void setMessageReceiver(hardwareMidiNativeReceiver receiver)
    {
        mAppMidiSupport.setMessageReceiver(receiver);
    }

    /**
     * Init the native midireception tier (here, connected to the Aeolus synthesizer)
     */
    public static void initNative() {
        mAppMidiSupport.initNative();


    }


    /**
     * Set a reference to the actual native interface
     * The idea here is that the actual native interface is provided by another module or
     * actually typically the main app
     */

    public static void setMidiNativeSetupHandler(hardwareMidiNativeSetup setupHandler)
    {
        mAppMidiSupport.setMidiNativeSetupHandler(setupHandler);
    }

    public static boolean isMidiNativeSetupHandlerSet()
    {
        return mAppMidiSupport.isMidiNativeSetupHandlerSet();
    }


    public static hardwareMidiNativeReceiver getDefaultMidiMessageReceiver() {
        return mAppMidiSupport;
    }
}
