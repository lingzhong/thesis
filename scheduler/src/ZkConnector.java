import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

public class ZkConnector implements Watcher {

	// ZooKeeper Object
	ZooKeeper zooKeeper;

	// To block any operation until ZooKeeper is connected. It's initialized
	// with count 1, that is, ZooKeeper connect state.
	CountDownLatch connectedSignal = new CountDownLatch(1);

	protected static final List<ACL> acl = Ids.OPEN_ACL_UNSAFE;

	/**
	 * Connects to ZooKeeper servers specified by hosts.
	 */
	public void connect(String hosts) throws IOException, InterruptedException {

		this.zooKeeper = new ZooKeeper(
				hosts, // ZooKeeper service hosts
				5000,  // Session timeout in milliseconds
				this); // watches -- this class implements Watcher; events are handled by the process method
		this.connectedSignal.await();
	}

	/**
	 * Closes connection with ZooKeeper
	 */
	public void close() throws InterruptedException {
		this.zooKeeper.close();
	}

	/**
	 * @return the zooKeeper
	 */
	public ZooKeeper getZooKeeper() {
		// Verify ZooKeeper's validity
		if (null == this.zooKeeper || !this.zooKeeper.getState().equals(States.CONNECTED)) {
			throw new IllegalStateException ("ZooKeeper is not connected.");
		}
		return this.zooKeeper;
	}

	@Override
	public void process(WatchedEvent event) {
		// check if event is for connection done; if so, unblock main thread blocked in connect
		if (event.getState() == KeeperState.SyncConnected) {
			this.connectedSignal.countDown();
		}
	}

	protected List<String> getChildren(String path, Watcher watcher) {
		List<String> childrenList = null;
		try {
			childrenList = this.zooKeeper.getChildren(path, watcher);
		} catch (Exception e) {
			System.err.println("Failed to get children for " + path);
		}
		return childrenList;
	}

	protected List<String> getChildren(String path) {
		List<String> childrenList = null;
		try {
			childrenList = this.zooKeeper.getChildren(path, false);
		} catch (Exception e) {
			System.err.println("Failed to get children for " + path);
		}
		return childrenList;
	}

	protected Stat exists(String path, Watcher watch) {
		Stat stat = null;
		try {
			stat = this.zooKeeper.exists(path, watch);
		} catch (Exception e) {

			System.out.println("Exception thrown at exists. watch not set");
		}
		return stat;
	}

	protected KeeperException.Code delete(String path, int version) {
		try {
			this.zooKeeper.delete(path, version);

		} catch (KeeperException e) {
			return e.code();
		} catch (Exception e) {
			return KeeperException.Code.SYSTEMERROR;
		}
		return KeeperException.Code.OK;
	}

	protected KeeperException.Code delete(String path) {
		try {
			this.zooKeeper.delete(path, -1);

		} catch (KeeperException e) {
			return e.code();
		} catch (Exception e) {
			return KeeperException.Code.SYSTEMERROR;
		}
		return KeeperException.Code.OK;
	}

	protected KeeperException.Code create(String path, String data,
			CreateMode mode) {
		try {
			byte[] byteData = null;
			if (data != null) {
				byteData = data.getBytes();
			}
			this.zooKeeper.create(path, byteData, acl, mode);
		} catch (KeeperException e) {
			return e.code();
		} catch (Exception e) {
			return KeeperException.Code.SYSTEMERROR;
		}

		return KeeperException.Code.OK;
	}
}

