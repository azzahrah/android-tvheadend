/*
 * Copyright (c) 2017 Kiall Mac Innes <kiall@macinnes.ie>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ie.macinnes.tvheadend.player.reader;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.ParsableByteArray;

import ie.macinnes.htsp.HtspMessage;

/**
 * A PlainStreamReader simply copies the raw bytes from muxpkt's over onto the track output
 */
abstract class PlainStreamReader implements StreamReader {
    private static final String TAG = PlainStreamReader.class.getName();

    protected TrackOutput mTrackOutput;

    @Override
    public final void createTracks(@NonNull HtspMessage stream, @NonNull ExtractorOutput output) {
        final int streamIndex = stream.getInteger("index");
        mTrackOutput = output.track(streamIndex);
        mTrackOutput.format(buildFormat(streamIndex, stream));
    }

    @Override
    public final void consume(@NonNull final HtspMessage message) {
        final long pts = message.getInteger("pts");
        final int frameType = message.getInteger("frametype");
        final byte[] payload = message.getByteArray("payload");

        final ParsableByteArray pba = new ParsableByteArray(payload);

        int bufferFlags = 0;

        Log.d(TAG, "PSR: frameType " + frameType);

        // Frame Type 66 = B
        // Frame Type 73 = I
        // Frame Type 80 = P
        if (frameType == 73) {
            // We have an I frame, tell exoplayer this as a keyframe
            Log.d(TAG, "PSR: Setting  BUFFER_FLAG_KEY_FRAME");
            bufferFlags |= C.BUFFER_FLAG_KEY_FRAME;
        }

        Log.d(TAG, "PSR: bufferFlags " + bufferFlags);

        mTrackOutput.sampleData(pba, payload.length);
        mTrackOutput.sampleMetadata(pts, bufferFlags, payload.length, 0, null);
    }

    @NonNull
    abstract protected Format buildFormat(int streamIndex, @NonNull HtspMessage stream);
}
