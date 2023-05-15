// Created by qiuwenchen on 2023/4/19.
//

/*
 * Tencent is pleased to support the open source community by making
 * WCDB available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *       https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.wcdb.core;

import com.tencent.wcdb.base.WCDBException;
import com.tencent.wcdb.winq.Statement;

public class Handle extends HandleORMOperation {
    private PreparedStatement mainStatement = null;
    private Database database = null;

    Handle(Database database) {
        this.database = database;
    }

    Handle(long cppObj, Database database) {
        this.cppObj = cppObj;
        this.database = database;
    }

    @Override
    public long getCppObj() throws WCDBException {
        if(cppObj == 0) {
            assert database != null;
            cppObj = database.getHandle(database.getCppObj());
            if(!checkValid(cppObj)) {
                throw database.createException();
            }
        }
        return cppObj;
    }

    WCDBException createException() {
        return new WCDBException(getError(getCppObj()));
    }

    native long getError(long self);

    static native boolean checkValid(long self);

    public PreparedStatement getOrCreatePreparedStatement(Statement statement) throws WCDBException {
        PreparedStatement preparedStatement = new PreparedStatement(getOrCreatePreparedStatement(getCppObj(), statement.getCppObj()));
        if(!preparedStatement.checkPrepared()) {
            throw createException();
        }
        return preparedStatement;
    }

    native long getOrCreatePreparedStatement(long self, long statement);

    public PreparedStatement preparedWithMainStatement(Statement statement) throws WCDBException {
        if(mainStatement == null) {
            mainStatement = new PreparedStatement(getMainStatement(getCppObj()));
            mainStatement.autoFinalize = true;
        }
        mainStatement.prepare(statement);
        return mainStatement;
    }

    native long getMainStatement(long self);

    public void finalizeAllStatements() {
        finalizeAllStatements(getCppObj());
    }

    native void finalizeAllStatements(long self);

    public void invalidate() {
        mainStatement = null;
        if(cppObj != 0) {
            releaseCPPObject(cppObj);
            cppObj = 0;
        }
    }

    native int tableExist(long self, String tableName);

    native boolean execute(long self, long statement);

    public int getChanges() {
        return getChanges(getCppObj());
    }

    native int getChanges(long self);

    public int getTotalChanges() {
        return getTotalChanges(getCppObj());
    }

    native int getTotalChanges(long self);

    public long getLastInsertedRowId() {
        return getLastInsertedRowId(getCppObj());
    }

    native long getLastInsertedRowId(long self);

    native boolean isInTransaction(long self);

    native boolean beginTransaction(long self);

    native boolean commitTransaction(long self);

    native void rollbackTransaction(long self);

    native boolean beginNestedTransaction(long self);

    native boolean commitNestedTransaction(long self);

    native void rollbackNestedTransaction(long self);

    @Override
    Handle getHandle() {
        return this;
    }

    @Override
    Database getDatabase() {
        return database;
    }

    @Override
    boolean autoInvalidateHandle() {
        return false;
    }

    private boolean onTransaction(long cppHandle, Transaction transaction) {
        Handle handle = new Handle(cppHandle, database);
        boolean ret = false;
        try {
            ret = transaction.insideTransaction(handle);
        } catch (WCDBException e) {
            ret = false;
        }
        return ret;
    }

    native boolean runTransaction(long self, Transaction transaction);

    native boolean runNestedTransaction(long self, Transaction transaction);

    private int onPausableTransaction(long cppHandle, PausableTransaction transaction, boolean isNewTransaction) {
        Handle handle = new Handle(cppHandle, database);
        int ret = 0;
        try {
            ret = transaction.insideTransaction(handle, isNewTransaction) ? 1 : 0;
        } catch (WCDBException e) {
            ret = 2;
        }
        return ret;
    }

    native boolean runPausableTransaction(long self, PausableTransaction transaction);

}
