package com.kixeye.chassis.chassis.test.hystrix;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rx.Observable;
import rx.Observer;

import com.kixeye.chassis.chassis.ChassisConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.exception.HystrixRuntimeException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ChassisHystrixTestConfiguration.class, ChassisConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChassisHystrixTest {

    private final HystrixCommandGroupKey hxGroupKey= HystrixCommandGroupKey.Factory.asKey("TestGroup");
    private final HystrixCommandKey hxTestCommandKey= HystrixCommandKey.Factory.asKey("TestCommand");
    private final HystrixCommandKey hxTestTimeoutCommandKey= HystrixCommandKey.Factory.asKey("TestTimeoutCommand");
    private final HystrixThreadPoolKey hxTestThreadPoolKey = HystrixThreadPoolKey.Factory.asKey("TestThreadPool");

    private final String resultString = "done!!";

    @Test
    public void testBasicHystrixCommand() throws Exception {

        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicReference<String> result = new AtomicReference<>(null);

        // kick off async command
        Observable<String> observable = new TestCommand().observe();

        // Sleep to test what happens if command finishes before subscription
        Thread.sleep(2000);

        // Add handler
        observable.subscribe( new Observer<String>() {
            @Override
            public void onCompleted() {
                done.set(true);
            }

            @Override
            public void onError(Throwable e) {
                result.set("error!!");
                done.set(true);
            }

            @Override
            public void onNext(String args) {
                result.set(args);
                done.set(true);
            }
        });

        // spin until done
        while (!done.get()) {
            Thread.sleep(100);
        }

        Assert.assertEquals(resultString, result.get());
    }

    @Test
    @SuppressWarnings("unused")
    public void testCommandTimeOut() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);

        // kick off async command
        Observable<String> observable = new TestTimeOutCommand().observe();

        // Add handler
        observable.subscribe( new Observer<String>() {
			@Override
            public void onCompleted() {
                Assert.assertNull("Should not complete");
                done.set(true);
            }

            @Override
            public void onError(Throwable e) {
                done.set(true);
            }

            @Override
            public void onNext(String args) {
                Assert.assertNull("Should not get a result");
                done.set(true);
            }
        });

        // spin until done
        while (!done.get()) {
            Thread.sleep(100);
        }
    }

    @Test
    public void testCircuitBreaker() {
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.TestCommand.circuitBreaker.forceOpen", true);
        boolean gotException = false;
        try {
            TestCommand cmd = new TestCommand();
            cmd.execute();
        } catch (HystrixRuntimeException ex) {
            gotException = true;
        } finally {
            ConfigurationManager.getConfigInstance().setProperty("hystrix.command.TestCommand.circuitBreaker.forceOpen", false);
        }
        Assert.assertTrue(gotException);
    }

    private class TestCommand extends HystrixCommand<String> {

        public TestCommand() {
            super(Setter
                    .withGroupKey(hxGroupKey)
                    .andThreadPoolKey(hxTestThreadPoolKey)
                    .andCommandKey(hxTestCommandKey) );
        }

        @Override
        protected String run() {
            try {
                Thread.sleep(1010);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return resultString;
        }
    }

    private class TestTimeOutCommand extends HystrixCommand<String> {

        public TestTimeOutCommand() {
            super(Setter
                    .withGroupKey(hxGroupKey)
                    .andThreadPoolKey(hxTestThreadPoolKey)
                    .andCommandKey(hxTestTimeoutCommandKey) );
        }

        @Override
        protected String run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            return resultString;
        }
    }
}
