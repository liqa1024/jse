package test.code

import jse.code.collection.IntDeque


def deque = new IntDeque();

for (i in 0..<100) {
    if (i%2 == 0) deque.addFirst(i);
    else deque.addLast(i);
}
for (i in 0..<50) {
    if (i%2 == 0) deque.removeLast();
    else deque.removeFirst();
}

deque.forEach {println(it);}

