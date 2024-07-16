package com.hmdp.utils;

public interface ILock {
    public boolean tryLock(long timeoutSec);

    public void unLock();
}
