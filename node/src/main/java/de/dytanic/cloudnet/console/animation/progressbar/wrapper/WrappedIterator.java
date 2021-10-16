/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.console.animation.progressbar.wrapper;

import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.progressbar.ConsoleProgressAnimation;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

public final class WrappedIterator<T> implements Iterator<T> {

  private final Iterator<T> wrapped;
  private final ConsoleProgressAnimation animation;

  public WrappedIterator(
    @NotNull Iterator<T> wrapped,
    @NotNull IConsole console,
    @NotNull ConsoleProgressAnimation animation
  ) {
    this.wrapped = wrapped;
    this.animation = animation;

    console.startAnimation(animation);
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = this.wrapped.hasNext();
    // close the animation if there are no more elements
    if (!hasNext) {
      this.animation.stepToEnd();
    }
    return hasNext;
  }

  @Override
  public T next() {
    T next = this.wrapped.next();
    this.animation.step();
    return next;
  }

  @Override
  public void remove() {
    this.wrapped.remove();
  }
}
