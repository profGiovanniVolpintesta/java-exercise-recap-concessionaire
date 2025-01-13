package volpintesta.test;

public class SyncField<E>
{
    private Object mutex;
    private E value;
    public E getValue() { synchronized(mutex) { return this.value; } }
    public void setValue(E value) { synchronized(mutex) { this.value = value; } }
    public SyncField(Object synchronizationMutex, E initializationValue)
    {
        value = initializationValue;
        mutex = synchronizationMutex;
    }
}
