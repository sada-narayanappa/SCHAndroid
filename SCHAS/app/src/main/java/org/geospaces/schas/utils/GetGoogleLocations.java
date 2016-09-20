package org.geospaces.schas.utils;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Erik on 9/11/2016.
 */
public class GetGoogleLocations {

    public List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList();

        parser.require(XmlPullParser.START_TAG, null, "kml");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            //starts by looking for a <gx:Track> tag
            if (name.equals("gx:Track")) {
                List<Entry> track = readEntry(parser);
                for (Entry location : track){
                    entries.add(location);
                }
            }
            //add more here like below if it is desired to pick up tags outside of track tags
            else{
                skip(parser);
            }
        }
        return entries;
    }

    //a class that holds all the data fields for an "entry" (location)
    public static class Entry {
        public final String location;

        private Entry(String pLocation){
            location = pLocation;
        }
    }

    private List readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        List locations = new ArrayList();

        parser.require(XmlPullParser.START_TAG, null, "gx:Track");
        while (parser.next() != XmlPullParser.START_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("gx:coord")) {
                String pLocation = readLocation(parser);
                locations.add(new Entry(pLocation));
            }
            //if more here if more tags are desired other than the coord tag within the track
            else {
                skip(parser);
            }
        }

        return locations;
    }

    //process coord tags in the kml
    private String readLocation(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "gx:coord");
        String pLocation = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "gx:coord");
        return pLocation;
    }

    private String readText(XmlPullParser parser) throws XmlPullParserException, IOException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;
        while (depth != 0){
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
