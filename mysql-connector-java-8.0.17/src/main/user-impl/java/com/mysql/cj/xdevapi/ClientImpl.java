/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 2.0, as published by the
 * Free Software Foundation.
 *
 * This program is also distributed with certain software (including but not
 * limited to OpenSSL) that is licensed under separate terms, as designated in a
 * particular file or component or in included license documentation. The
 * authors of MySQL hereby grant you an additional permission to link the
 * program and your derivative works with the separately licensed software that
 * they have included with MySQL.
 *
 * Without limiting anything contained in the foregoing, this file, which is
 * part of MySQL Connector/J, is also subject to the Universal FOSS Exception,
 * version 1.0, a copy of which can be found at
 * http://oss.oracle.com/licenses/universal-foss-exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License, version 2.0,
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301  USA
 */

package com.mysql.cj.xdevapi;

import com.mysql.cj.conf.*;
import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.exceptions.CJException;
import com.mysql.cj.exceptions.WrongArgumentException;
import com.mysql.cj.protocol.x.XProtocol;
import com.mysql.cj.util.StringUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientImpl implements Client {
    boolean isClosed = false;

    private ConnectionUrl connUrl = null;

    private boolean poolingEnabled = true;
    private int maxSize = 25;
    int maxIdleTime = 0;
    private int queueTimeout = 0;

    BlockingQueue<PooledXProtocol> idleProtocols = null;
    Set<WeakReference<PooledXProtocol>> activeProtocols = null;

    Set<WeakReference<Session>> nonPooledSessions = null;

    SessionFactory sessionFactory = new SessionFactory();

    public ClientImpl(String url, String clientPropsJson) {
        Properties clientProps = StringUtils.isNullOrEmpty(clientPropsJson) ? new Properties() : clientPropsFromJson(clientPropsJson);
        init(url, clientProps);
    }

    public ClientImpl(String url, Properties clientProps) {
        init(url, clientProps != null ? clientProps : new Properties());
    }

    private Properties clientPropsFromJson(String clientPropsJson) {
        Properties props = new Properties();
        DbDoc clientPropsDoc = JsonParser.parseDoc(clientPropsJson);

        JsonValue pooling = clientPropsDoc.remove("pooling");
        if (pooling != null) {
            if (!DbDoc.class.isAssignableFrom(pooling.getClass())) {
                throw new XDevAPIError(String.format("Client option 'pooling' does not support value '%s'.", pooling.toFormattedString()));
            }
            DbDoc poolingDoc = ((DbDoc) pooling);
            JsonValue jsonVal;

            jsonVal = poolingDoc.remove("enabled");
            if (jsonVal != null) {
                if (JsonLiteral.class.isAssignableFrom(jsonVal.getClass())) {
                    JsonLiteral pe = (JsonLiteral) jsonVal;
                    if (pe != JsonLiteral.FALSE && pe != JsonLiteral.TRUE) {
                        throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_ENABLED.getKeyName(),
                                jsonVal.toFormattedString()));
                    }
                    props.setProperty(ClientProperty.POOLING_ENABLED.getKeyName(), pe.value);
                } else if (JsonString.class.isAssignableFrom(jsonVal.getClass())) {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_ENABLED.getKeyName(),
                            ((JsonString) jsonVal).getString()));
                } else {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_ENABLED.getKeyName(),
                            jsonVal.toFormattedString()));
                }
            }
            jsonVal = poolingDoc.remove("maxSize");
            if (jsonVal != null) {
                if (JsonNumber.class.isAssignableFrom(jsonVal.getClass())) {
                    props.setProperty(ClientProperty.POOLING_MAX_SIZE.getKeyName(), ((JsonNumber) jsonVal).toString());
                } else if (JsonString.class.isAssignableFrom(jsonVal.getClass())) {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_MAX_SIZE.getKeyName(),
                            ((JsonString) jsonVal).getString()));
                } else {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_MAX_SIZE.getKeyName(),
                            jsonVal.toFormattedString()));
                }
            }
            jsonVal = poolingDoc.remove("maxIdleTime");
            if (jsonVal != null) {
                if (JsonNumber.class.isAssignableFrom(jsonVal.getClass())) {
                    props.setProperty(ClientProperty.POOLING_MAX_IDLE_TIME.getKeyName(), ((JsonNumber) jsonVal).toString());
                } else if (JsonString.class.isAssignableFrom(jsonVal.getClass())) {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_MAX_IDLE_TIME.getKeyName(),
                            ((JsonString) jsonVal).getString()));
                } else {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_MAX_IDLE_TIME.getKeyName(),
                            jsonVal.toFormattedString()));
                }
            }
            jsonVal = poolingDoc.remove("queueTimeout");
            if (jsonVal != null) {
                if (JsonNumber.class.isAssignableFrom(jsonVal.getClass())) {
                    props.setProperty(ClientProperty.POOLING_QUEUE_TIMEOUT.getKeyName(), ((JsonNumber) jsonVal).toString());
                } else if (JsonString.class.isAssignableFrom(jsonVal.getClass())) {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_QUEUE_TIMEOUT.getKeyName(),
                            ((JsonString) jsonVal).getString()));
                } else {
                    throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", ClientProperty.POOLING_QUEUE_TIMEOUT.getKeyName(),
                            jsonVal.toFormattedString()));
                }
            }
            if (poolingDoc.size() > 0) {
                String key = poolingDoc.keySet().stream().findFirst().get();
                throw new XDevAPIError(String.format("Client option 'pooling.%s' is not recognized as valid.", key));
            }
        }

        if (!clientPropsDoc.isEmpty()) {
            String key = clientPropsDoc.keySet().stream().findFirst().get();
            throw new XDevAPIError(String.format("Client option '%s' is not recognized as valid.", key));
        }

        return props;
    }

    private void validateAndInitializeClientProps(Properties clientProps) {
        String propKey = "";
        String propValue = "";
        propKey = ClientProperty.POOLING_ENABLED.getKeyName();
        if (clientProps.containsKey(propKey)) {
            propValue = clientProps.getProperty(propKey);
            try {
                this.poolingEnabled = BooleanPropertyDefinition.booleanFrom(propKey, propValue, null);
            } catch (CJException e) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue), e);
            }
        }

        propKey = ClientProperty.POOLING_MAX_SIZE.getKeyName();
        if (clientProps.containsKey(propKey)) {
            propValue = clientProps.getProperty(propKey);
            try {
                this.maxSize = IntegerPropertyDefinition.integerFrom(propKey, propValue, 1, null);
            } catch (WrongArgumentException e) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue), e);
            }
            if (this.maxSize <= 0) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue));
            }
        }

        propKey = ClientProperty.POOLING_MAX_IDLE_TIME.getKeyName();
        if (clientProps.containsKey(propKey)) {
            propValue = clientProps.getProperty(propKey);
            try {
                this.maxIdleTime = IntegerPropertyDefinition.integerFrom(propKey, propValue, 1, null);
            } catch (WrongArgumentException e) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue), e);
            }
            if (this.maxIdleTime < 0) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue));
            }
        }

        propKey = ClientProperty.POOLING_QUEUE_TIMEOUT.getKeyName();
        if (clientProps.containsKey(propKey)) {
            propValue = clientProps.getProperty(propKey);
            try {
                this.queueTimeout = IntegerPropertyDefinition.integerFrom(propKey, propValue, 1, null);
            } catch (WrongArgumentException e) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue), e);
            }
            if (this.queueTimeout < 0) {
                throw new XDevAPIError(String.format("Client option '%s' does not support value '%s'.", propKey, propValue));
            }
        }

        List<String> clientPropsAsString = Stream.of(ClientProperty.values()).map(ClientProperty::getKeyName).collect(Collectors.toList());
        propKey = (String) clientProps.keySet().stream().filter(k -> !clientPropsAsString.contains(k)).findFirst().orElse(null);
        if (propKey != null) {
            throw new XDevAPIError(String.format("Client option '%s' is not recognized as valid.", propKey));
        }
    }

    private void init(String url, Properties clientProps) {
        this.connUrl = this.sessionFactory.parseUrl(url);

        validateAndInitializeClientProps(clientProps);

        if (this.poolingEnabled) {
            this.idleProtocols = new LinkedBlockingQueue<>(this.maxSize);
            this.activeProtocols = new HashSet<>(this.maxSize);
        } else {
            this.nonPooledSessions = new HashSet<>();
        }
    }

    @Override
    public Session getSession() {
        if (this.isClosed) {
            throw new XDevAPIError("Client is closed.");
        }

        if (!this.poolingEnabled) {
            // Remove nulled and closed session references from the nonPooledSessions set.
            List<WeakReference<Session>> obsoletedSessions = new ArrayList<>();
            for (WeakReference<Session> ws : this.nonPooledSessions) {
                if (ws != null) {
                    Session s = ws.get();
                    if (s == null || !s.isOpen()) {
                        obsoletedSessions.add(ws);
                    }
                }
            }
            for (WeakReference<Session> ws : obsoletedSessions) {
                this.nonPooledSessions.remove(ws);
            }

            Session sess = this.sessionFactory.getSession(this.connUrl);
            this.nonPooledSessions.add(new WeakReference<>(sess));
            return sess;
        }

        PooledXProtocol prot = null;

        long start = System.currentTimeMillis();
        while (prot == null && (this.queueTimeout == 0 || System.currentTimeMillis() < start + this.queueTimeout)) { // TODO how to avoid endless loop?
            synchronized (this.idleProtocols) {

                if (this.idleProtocols.peek() != null) {
                    // 1. If there are idle Protocols then return one of them. 
                    PooledXProtocol tryProt = this.idleProtocols.poll();
                    if (tryProt.isOpen()) { // ignore closed Session, try next idle Session
                        if (tryProt.isIdleTimeoutReached()) {
                            tryProt.realClose(); // close expired Session, try next idle Session
                        } else {
                            prot = tryProt;
                        }

                    }

                } else if (this.idleProtocols.size() + this.activeProtocols.size() < this.maxSize) {
                    // 2. No idle Protocols but the pool has free space. Adding new Protocol to pool.
                    CJCommunicationsException latestException = null;
                    for (HostInfo hi : this.connUrl.getHostsList()) {
                        PooledXProtocol tryProt = null;
                        try {
                            PropertySet pset = new DefaultPropertySet();
                            pset.initializeProperties(hi.exposeAsProperties());
                            tryProt = new PooledXProtocol(hi, pset);
                            tryProt.connect(hi.getUser(), hi.getPassword(), hi.getDatabase());
                            prot = tryProt;
                            break;
                        } catch (CJCommunicationsException e) {
                            latestException = e;
                        }
                    }
                    if (prot == null && latestException != null) {
                        throw latestException;
                    }

                } else if (this.queueTimeout > 0) {
                    // 3. No idle Protocols, no free space in the pool. Waiting queueTimeout milliseconds for idle Protocol.
                    long currentTimeout = this.queueTimeout - (System.currentTimeMillis() - start);
                    try {
                        if (currentTimeout > 0) {
                            prot = this.idleProtocols.poll(currentTimeout, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException e) {
                        throw new XDevAPIError("Session can not be obtained within " + this.queueTimeout + " milliseconds.", e);
                    }

                } else {
                    // 4. No idle Protocols, no free space in the pool. Waiting indefinitely for idle Protocol.
                    prot = this.idleProtocols.poll(); // TODO endless lock ?
                }
            }
        }
        if (prot == null) {
            throw new XDevAPIError("Session can not be obtained within " + this.queueTimeout + " milliseconds.");
        }
        this.activeProtocols.add(new WeakReference<>(prot));
        SessionImpl sess = new SessionImpl(prot);
        return sess;
    }

    @Override
    public void close() {
        if (this.poolingEnabled) {
            synchronized (this.idleProtocols) {
                if (!this.isClosed) {
                    this.isClosed = true;
                    this.idleProtocols.forEach(s -> s.realClose());
                    this.idleProtocols.clear();
                    this.activeProtocols.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(s -> s.realClose());
                    this.activeProtocols.clear();
                }
            }
        } else {
            this.nonPooledSessions.stream().map(WeakReference::get).filter(Objects::nonNull).filter(Session::isOpen).forEach(s -> s.close());
        }
    }

    void idleProtocol(PooledXProtocol sess) {
        synchronized (this.idleProtocols) {
            if (!this.isClosed) {
                List<WeakReference<PooledXProtocol>> removeThem = new ArrayList<>();
                for (WeakReference<PooledXProtocol> wps : this.activeProtocols) {
                    if (wps != null) {
                        PooledXProtocol as = wps.get();
                        if (as == null) {
                            removeThem.add(wps);
                        } else if (as == sess) {
                            removeThem.add(wps);
                            this.idleProtocols.add(as);
                        }
                    }
                }

                for (WeakReference<PooledXProtocol> wr : removeThem) {
                    this.activeProtocols.remove(wr);
                }
            }
        }
    }

    public class PooledXProtocol extends XProtocol {

        long idleSince = -1;

        public PooledXProtocol(HostInfo hostInfo, PropertySet propertySet) {
            super(hostInfo, propertySet);
        }

        @Override
        public void close() {
            reset();
            this.idleSince = System.currentTimeMillis();
            idleProtocol(this);
        }

        boolean isIdleTimeoutReached() {
            return ClientImpl.this.maxIdleTime > 0 && this.idleSince > 0 && System.currentTimeMillis() > this.idleSince + ClientImpl.this.maxIdleTime;
        }

        void realClose() {
            try {
                super.close();
            } catch (IOException e) {
                // TODO is it really no-op?
            }
        }

    }

}
