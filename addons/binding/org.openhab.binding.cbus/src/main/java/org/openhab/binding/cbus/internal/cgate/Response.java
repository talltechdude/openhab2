/**
 *  CGateInterface - A library to allow interaction with Clipsal C-Gate.
 *  Copyright (C) 2008,2009,2012  Dave Oxley <dave@daveoxley.co.uk>.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openhab.binding.cbus.internal.cgate;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dave Oxley <dave@daveoxley.co.uk>
 */
public class Response implements Iterable<String> {
    private Logger logger = LoggerFactory.getLogger(Response.class);

    private final Object response_mutex = new Object();

    private final Object iterator_mutex = new Object();

    private BufferedReader response_reader;

    private Thread response_thread;

    private ArrayList<String> array_response;

    private boolean response_generated = false;

    Response(BufferedReader response_reader) throws CGateException {
        try {
            this.response_reader = response_reader;
            this.response_thread = new Thread() {
                @Override
                public void run() {
                    synchronized (iterator_mutex) {
                        array_response = new ArrayList<String>();
                    }

                    try {
                        boolean has_more = true;
                        while (has_more) {
                            String response = Response.this.response_reader.readLine();
                            synchronized (Response.this.iterator_mutex) {
                                array_response.add(response);
                                Response.this.iterator_mutex.notifyAll();
                            }
                            has_more = responseHasMore(response);
                        }

                        if (logger.isDebugEnabled()) {
                            for (String response : array_response) {
                                logger.debug("response: " + response);
                            }
                        }
                    } catch (IOException e) {
                        new CGateException(e);
                    } finally {
                        Response.this.response_reader = null;
                        Response.this.response_thread = null;
                        synchronized (Response.this.response_mutex) {
                            synchronized (Response.this.iterator_mutex) {
                                Response.this.response_generated = true;
                                Response.this.response_mutex.notifyAll();
                                Response.this.iterator_mutex.notifyAll();
                            }
                        }
                    }
                }
            };
            this.response_thread.start();
        } catch (Exception e) {
            throw new CGateException(e);
        }
    }

    static boolean responseHasMore(String response) {
        return response.substring(3, 4).equals("-");
    }

    ArrayList<String> toArray() {
        synchronized (response_mutex) {
            while (!response_generated) {
                try {
                    response_mutex.wait();
                } catch (InterruptedException ie) {
                }
            }
        }

        return array_response;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                synchronized (iterator_mutex) {
                    while (array_response == null || (index >= array_response.size() && !response_generated)) {
                        try {
                            iterator_mutex.wait();
                        } catch (InterruptedException ie) {
                        }
                    }

                    if (index < array_response.size())
                        return true;

                    if (response_generated)
                        return false;

                    throw new IllegalStateException("Impossible");
                }
            }

            @Override
            public String next() {
                synchronized (iterator_mutex) {
                    if (index >= array_response.size())
                        throw new NoSuchElementException();

                    return array_response.get(index++);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    public void handle200() throws CGateException {
        ArrayList<String> resp_array = toArray();
        if (resp_array.isEmpty())
            throw new CGateException();

        String resp_str = resp_array.get(resp_array.size() - 1);
        String result_code = resp_str.substring(0, 3).trim();
        if (!result_code.equals("200"))
            throw new CGateException(resp_str);
    }
}
