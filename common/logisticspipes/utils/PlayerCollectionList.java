package logisticspipes.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PlayerCollectionList {

	private List<EqualWeakReference<EntityPlayer>> players = new ArrayList<EqualWeakReference<EntityPlayer>>();
	private boolean checkingPlayers =false;

	public void checkPlayers() {
		checkingPlayers = true;
		Iterator<EqualWeakReference<EntityPlayer>> iPlayers = players.iterator();
		while(iPlayers.hasNext()) {
			EqualWeakReference<EntityPlayer> playerReference = iPlayers.next();
			boolean remove = false;
			if(playerReference.get() == null) {
				remove = true;
			} else if(playerReference.get().isDead) {
				remove = true;
			} else if(playerReference.get() instanceof EntityPlayerMP) {
				if(((EntityPlayerMP)playerReference.get()).playerNetServerHandler.connectionClosed) {
					remove = true;
				}
			}
			if(remove) {
				iPlayers.remove();
			}
		}
		checkingPlayers = false;
	}

	public Iterable<EntityPlayer> players() {
		checkPlayers();
		return new Iterable<EntityPlayer>() {
			@Override
			public Iterator<EntityPlayer> iterator() {
				return new Itr(players.iterator());
			}
			
		};
	}

	public int size() {
		if(!checkingPlayers)
			checkPlayers();
		return players.size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public void add(EntityPlayer player) {
		players.add(new EqualWeakReference<EntityPlayer>(player));
	}

	public boolean remove(EntityPlayer o) {
		return players.remove(new EqualWeakReference<EntityPlayer>(o));
	}

	private class Itr implements Iterator<EntityPlayer> {
		
		private final Iterator<EqualWeakReference<EntityPlayer>> iterator;
		
		private Itr(Iterator<EqualWeakReference<EntityPlayer>> source) {
			this.iterator = source;
		}
		
		public boolean hasNext() {
			return iterator.hasNext();
		}
		
		public EntityPlayer next() {
			EqualWeakReference<EntityPlayer> reference = iterator.next();
			return (EntityPlayer) reference.get();
		}
		
		public void remove() {
        	iterator.remove();
        }
	}
}
