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
// ----------------------------------------------------------------------------

package com.mathis.inputMidi.softwareMidi;

import android.media.midi.MidiReceiver;

import android.util.Log;

import com.mathis.inputMidi.MidiSpec;

/**
 * Helper class for MidiSynthDeviceService. It is a requirement by the software midi framework
 * of Android ("virtual midi ports") to have a message receiver object implementing MidiReceiver,
 * one per open receiving port. Here, the implementation is minimalistic, the incoming messages are
 * copied and transmitted to the softwareMidiReceiver object configured; optionally, transmission
 * to the softwareMidiReceiver object can be shut off.
 */
public class SynthDeviceServiceMidiReceiver extends MidiReceiver {

    /**
     * The receiver object to which the midi messages will be transmitted
     */
    protected softwareMidiReceiver receiver=null;
    /**
     * Switch to toggle whether we want to receive software midi messages
     */
    protected boolean transmitSoftwareMidiMessagesToReceiver = true;

    /**
     * Set whether incoming midi messages shall be transmitted to the softwareMidiReceiver target
     * or not
     * @param doReceive If true, transmit, otherwise, don't
     */
    public void setTransmitSoftwareMidiMessagesToReceiver(boolean doReceive)
    {
        transmitSoftwareMidiMessagesToReceiver = doReceive;
    }

    /**
     * Are we transmitting the messages to the softwareMidiReceiver?
     * @return
     */
    public boolean getTransmitSoftwareMidiMessagesToReceiver()
    {
        return transmitSoftwareMidiMessagesToReceiver;
    }

    /**
     * Mandatory midi handling function to extend Midireceiver. Despite its suggestive name,
     * this function is
     * called when an incoming midi message is received by the Android midi framework
     * @param msg a byte array containing the MIDI data
     * @param offset the offset of the first byte of the data in the array to be processed
     * @param count the number of bytes of MIDI data in the array to be processed
     * @param timestamp the timestamp of the message (based on {@link java.lang.System#nanoTime}
     */

    @Override
    public void onSend(byte[] msg, int offset, int count, long timestamp) {

        if(!(receiver==null) & (count > 0))
       {
           if(transmitSoftwareMidiMessagesToReceiver) {
               // if the message is a note-on or note-off message, three bytes are required.
               if(((msg[0] & 0xF0) >> 4 == MidiSpec.MIDICODE_NOTEON)|((msg[0] & 0xF0) >> 4 == MidiSpec.MIDICODE_NOTEOFF))
               {
                  if(count<3){
                      Log.i("SynthDeviceServiceMidiReceiver::onSend","Note on/off message too short! Needs to contain 3 bytes");
                      return;
                  }

               }
               byte[] submessage = new byte[count];
               for (int i = 0; i < count; i++) {
                   submessage[i] = msg[i + offset];
               }


               receiver.onSoftwareMidiMessageReceive(submessage);
           }
       }
    }

    /** Set the SoftwareMidiReceiver object. This is the object to which incoming
     * midi messages will be transmitted (as an array of bytes)
     * @param rec The receiving object
     */

    public void setSoftwareMidiMessageReceiver(softwareMidiReceiver rec) {
        Log.i("SynthDeviceServiceMidiReceiver","setSoftwareMidiMessageReceiver");
        receiver=rec;
    }

    /**
     * Remove the current softwareMidiReceiver object
     */

    public void removeSoftwareMidiMessageReceiver()
    {
        receiver=null;
    }

    /** force setting of the software midi device status
     * @param active Known state of the device, for example at re-creation of objects due to change of orientation
     */
    public void onDeviceStatusChanged(boolean active)
    {
        if(!(receiver==null))
        {
            receiver.onDeviceStatusChanged(active);
        }
    }
}