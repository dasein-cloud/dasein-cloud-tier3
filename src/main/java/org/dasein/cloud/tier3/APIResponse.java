package org.dasein.cloud.tier3;

import org.apache.http.HttpStatus;
import org.dasein.cloud.CloudException;
import org.dasein.util.CalendarWrapper;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public class APIResponse {
    static public enum ResponseType { XML, JSON, RAW, NONE }

    private int         code;
    private Boolean     complete;
    private JSONObject  json;
    private InputStream data;
    private Document    xml;

    private CloudException error;
    private APIResponse next;

    public APIResponse() { }

    /**
     * @return the HTTP code provided in the response
     * @throws CloudException an error occurred parsing the response
     */
    public int getCode() throws CloudException {
        synchronized( this ) {
            while( complete == null && error == null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            return code;
        }
    }

    /**
     * @return the raw input stream of the response body
     * @throws CloudException an error occurred parsing the response
     */
    public @Nullable InputStream getData() throws CloudException {
        synchronized( this ) {
            while( complete == null && error == null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            return data;
        }
    }

    /**
     * @return the response body as a JSON object
     * @throws CloudException an error occurred parsing the response
     */
    public @Nullable JSONObject getJSON() throws CloudException {
        synchronized( this ) {
            while( complete == null && error ==null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            return json;
        }
    }

    /**
     * @return the type of content returned in the response body
     * @throws CloudException an error occurred parsing the response
     */
    public @Nonnull ResponseType getResponseType() throws CloudException {
        synchronized( this ) {
            while( complete == null && error ==null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            if( json != null ) {
                return ResponseType.JSON;
            }
            if( xml != null ) {
                return ResponseType.XML;
            }
            if( data != null ) {
                return ResponseType.RAW;
            }
            return ResponseType.NONE;
        }
    }

    /**
     * @return the response body as an XML document
     * @throws CloudException an error occurred parsing the response
     */
    public @Nullable Document getXML() throws CloudException {
        synchronized( this ) {
            while( complete == null && error ==null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            return xml;
        }
    }

    /**
     * @return whether or not the response parsing has completed
     * @throws CloudException an error occurred parsing the response
     */
    public boolean isComplete() throws CloudException {
        synchronized( this ) {
            while( complete == null && error == null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            return complete;
        }
    }

    /**
     * @return provides the next page in a multi-page API response (currently, this is always null for ASM)
     * @throws CloudException an error occurred parsing the response
     */
    public @Nullable APIResponse next() throws CloudException {
        synchronized( this ) {
            while( complete == null && error == null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            if( complete ) {
                return null;
            }
            while( next == null && error == null ) {
                try { wait(CalendarWrapper.MINUTE); }
                catch( InterruptedException ignore ) { }
            }
            if( error != null ) {
                throw error;
            }
            return next;
        }
    }

    /**
     * Receives a NOT FOUND/404 response from the server and marks the response complete.
     */
    void receive() {
        synchronized( this ) {
            this.code = HttpStatus.SC_NOT_FOUND;
            this.complete = true;
            notifyAll();
        }
    }

    /**
     * Receives an error from ASM and marks the response complete.
     * @param error the error received from ASM
     */
    void receive(CloudException error) {
        synchronized( this ) {
            this.code = error.getHttpCode();
            this.error = error;
            this.complete = true;
            notifyAll();
        }
    }

    /**
     * Receives raw data from ASM and marks the response complete.
     * @param statusCode the HTTP status code for the response
     * @param data the raw input stream in the response body
     */
    void receive(int statusCode, @Nonnull InputStream data) {
        synchronized( this ) {
            this.code = statusCode;
            this.data = data;
            this.complete = true;
            notifyAll();
        }
    }

    /**
     * Receives a JSON object from the response body.
     * @param statusCode the HTTP status code for the response
     * @param json the response body as a JSON object
     * @param complete whether or not the response is complete
     */
    void receive(int statusCode, @Nonnull JSONObject json, boolean complete) {
        synchronized( this ) {
            this.code = statusCode;
            this.json = json;
            this.complete = complete;
            notifyAll();
        }
    }

    /**
     * Receives an XML document from the response body.
     * @param statusCode the HTTP status code for the response
     * @param xml the response body as an XML document
     * @param complete whether or not the response is complete
     */
    void receive(int statusCode, @Nonnull Document xml, boolean complete) {
        synchronized( this ) {
            this.code = statusCode;
            this.xml = xml;
            this.complete = complete;
            notifyAll();
        }
    }

    /**
     * Indicates the next part of a multi-page response.
     * @param next the next page in the multi-page response
     */
    void setNext(APIResponse next) {
        synchronized( this ) {
            this.next = next;
            notifyAll();
        }
    }
}
