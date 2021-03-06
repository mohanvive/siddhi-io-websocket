/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.extension.siddhi.io.websocket.sink;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.io.websocket.sink.util.ResultContainer;
import org.wso2.extension.siddhi.io.websocket.sink.util.WebSocketReceiver;
import org.wso2.extension.siddhi.io.websocket.util.WebSocketServer;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketSinkTest {
    private AtomicInteger eventCount = new AtomicInteger(0);

    @BeforeMethod
    public void init() {
        eventCount.set(0);
        WebSocketServer.start();
    }

    @AfterMethod
    public void serverClose() throws Exception {
        WebSocketServer.stop();
    }

    @Test
    public void testWebSocketSinkXmlMapTestCase() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        ResultContainer resultContainer = new ResultContainer(2);
        new WebSocketReceiver("ws://localhost:8080/chat/wso2", resultContainer);
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(
                "@App:name('TestExecutionPlan') " +
                        "define stream FooStream1 (symbol string, age int, country string); " +
                        "@info(name = 'query1') " +
                        "@sink(type='websocket', url = 'ws://localhost:8080/chat/wso2', " +
                        "@map(type='xml'))" +
                        "Define stream BarStream1 (symbol string, age int, country string);" +
                        "from FooStream1 select symbol, age, country insert into BarStream1;");
        InputHandler fooStream = executionPlanRuntime.getInputHandler("FooStream1");
        executionPlanRuntime.start();
        fooStream.send(new Object[]{"JAMES", 23, "USA"});
        fooStream.send(new Object[]{"MIKE", 23, "Germany"});
        Assert.assertTrue(resultContainer.assertMessageContent("JAMES"));
        Assert.assertTrue(resultContainer.assertMessageContent("MIKE"));
        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void testWebSocketSinkWithoutUri() {
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.createSiddhiAppRuntime(
                "@App:name('TestExecutionPlan') " +
                        "define stream FooStream1 (symbol string, price float, volume long); " +
                        "@info(name = 'query1') " +
                        "@sink(type='websocket', " +
                        "@map(type='xml'))" +
                        "Define stream BarStream1 (symbol string, price float, volume long);" +
                        "from FooStream1 select symbol, price, volume insert into BarStream1;");
    }

    @Test(expectedExceptions = SiddhiAppRuntimeException.class)
    public void testWebSocketSinkInvalidUri() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(
                "@App:name('TestExecutionPlan') " +
                        "define stream FooStream1 (symbol string, price float, volume long); " +
                        "@info(name = 'query1') " +
                        "@sink(type='websocket', url = 'ws://localhost:7070/websockets/abc'," +
                        "@map(type='xml'))" +
                        "Define stream BarStream1 (symbol string, price float, volume long);" +
                        "from FooStream1 select symbol, price, volume insert into BarStream1;");
        InputHandler fooStream = executionPlanRuntime.getInputHandler("FooStream1");
        executionPlanRuntime.start();
        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});
        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppRuntimeException.class)
    public void testWebSocketSinkInvalidHeaderFormat() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(
                "@App:name('TestExecutionPlan') " +
                        "define stream FooStream1 (symbol string, price float, volume long); " +
                        "@info(name = 'query1') " +
                        "@sink(type='websocket', url = 'ws://localhost:8080/chat/abc'," +
                        "headers=\"'message-type-websocket','message-sender:wso2'\", @map(type='xml'))" +
                        "Define stream BarStream1 (symbol string, price float, volume long);" +
                        "from FooStream1 select symbol, price, volume insert into BarStream1;");
        InputHandler fooStream = executionPlanRuntime.getInputHandler("FooStream1");
        executionPlanRuntime.start();
        fooStream.send(new Object[]{"WSO2", 55.6f, 100L});
        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testWebSocketSinkInvalidUrlScheme() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.createSiddhiAppRuntime(
                "@App:name('TestExecutionPlan') " +
                        "define stream FooStream1 (symbol string, price float, volume long); " +
                        "@info(name = 'query1') " +
                        "@sink(type='websocket', url = 'tcp://localhost:8025/abc'," +
                        "@map(type='xml'))" +
                        "Define stream BarStream1 (symbol string, price float, volume long);" +
                        "from FooStream1 select symbol, price, volume insert into BarStream1;");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testWebSocketSinkInvalidIdleTimeout() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.createSiddhiAppRuntime(
                "@App:name('TestExecutionPlan') " +
                        "define stream FooStream1 (symbol string, price float, volume long); " +
                        "@info(name = 'query1') " +
                        "@sink(type='websocket', url = 'ws://localhost:8080/chat/abc', idle.timeout = '-10'," +
                        "@map(type='xml'))" +
                        "Define stream BarStream1 (symbol string, price float, volume long);" +
                        "from FooStream1 select symbol, price, volume insert into BarStream1;");
    }
}
