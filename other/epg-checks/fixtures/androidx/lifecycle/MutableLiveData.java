package androidx.lifecycle; public class MutableLiveData<T> extends LiveData<T> { public void setValue(T v){value=v;} public void postValue(T v){value=v;} }
