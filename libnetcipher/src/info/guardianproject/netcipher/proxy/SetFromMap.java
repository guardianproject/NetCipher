/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package info.guardianproject.netcipher.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class SetFromMap<E> extends AbstractSet<E>
  implements Serializable {
  private static final long serialVersionUID = 2454657854757543876L;
  // Must be named as is, to pass serialization compatibility test.
  private final Map<E, Boolean> m;
  private transient Set<E> backingSet;
  SetFromMap(final Map<E, Boolean> map) {
    m = map;
    backingSet = map.keySet();
  }
  @Override public boolean equals(Object object) {
    return backingSet.equals(object);
  }
  @Override public int hashCode() {
    return backingSet.hashCode();
  }
  @Override public boolean add(E object) {
    return m.put(object, Boolean.TRUE) == null;
  }
  @Override public void clear() {
    m.clear();
  }
  @Override public String toString() {
    return backingSet.toString();
  }
  @Override public boolean contains(Object object) {
    return backingSet.contains(object);
  }
  @Override public boolean containsAll(Collection<?> collection) {
    return backingSet.containsAll(collection);
  }
  @Override public boolean isEmpty() {
    return m.isEmpty();
  }
  @Override public boolean remove(Object object) {
    return m.remove(object) != null;
  }
  @Override public boolean retainAll(Collection<?> collection) {
    return backingSet.retainAll(collection);
  }
  @Override public Object[] toArray() {
    return backingSet.toArray();
  }
  @Override
  public <T> T[] toArray(T[] contents) {
    return backingSet.toArray(contents);
  }
  @Override public Iterator<E> iterator() {
    return backingSet.iterator();
  }
  @Override public int size() {
    return m.size();
  }
  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    backingSet = m.keySet();
  }
}