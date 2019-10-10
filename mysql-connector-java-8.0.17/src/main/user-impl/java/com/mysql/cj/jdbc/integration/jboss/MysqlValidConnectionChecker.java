/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.mysql.cj.jdbc.integration.jboss;

import org.jboss.resource.adapter.jdbc.ValidConnectionChecker;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A more efficient connection checker for JBoss.
 */
public final class MysqlValidConnectionChecker implements ValidConnectionChecker, Serializable {

    private static final long serialVersionUID = 8909421133577519177L;

    public MysqlValidConnectionChecker() {
    }

    @Override
    public SQLException isValidConnection(Connection conn) {

        // Use "/* ping */ SELECT 1" which will send pings across multi-connections too in case the connection was "wrapped" by Jboss in any way...

        Statement pingStatement = null;

        try {
            pingStatement = conn.createStatement();

            pingStatement.executeQuery("/* ping */ SELECT 1").close();

            return null;
        } catch (SQLException sqlEx) {
            return sqlEx;
        } finally {
            if (pingStatement != null) {
                try {
                    pingStatement.close();
                } catch (SQLException sqlEx) {
                    // can't do anything about it here
                }
            }
        }
    }
}
