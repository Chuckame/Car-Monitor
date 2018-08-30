package org.michoko.lazyconnectionclient;

import org.michocko.lazyio.messaging.IConnection;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import lombok.Getter;

public class MainActivityViewModel extends ViewModel {

    @Getter
    private LiveData<List<Object>> liveDataListeMessages;

    MainActivityViewModel() {
        liveDataListeMessages = new MutableLiveData<>();

        // instanciation et lancement de ton écouteur réseau (voire dans une autre méthode, appelée par l'activity)
        // attention, l'écoute réseau doit se faire dans un thread à part
    }

    public void connect(String serialPort) {

    }

    public void disconnect() {

    }
}
