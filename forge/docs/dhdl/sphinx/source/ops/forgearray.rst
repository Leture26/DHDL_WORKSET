
.. role:: black
.. role:: gray
.. role:: silver
.. role:: white
.. role:: maroon
.. role:: red
.. role:: fuchsia
.. role:: pink
.. role:: orange
.. role:: yellow
.. role:: lime
.. role:: green
.. role:: olive
.. role:: teal
.. role:: cyan
.. role:: aqua
.. role:: blue
.. role:: navy
.. role:: purple

.. _ForgeArray:

ForgeArray
==========

<auto-generated stub>

Infix methods
-------------

.. parsed-literal::

  :maroon:`def` apply(i: :doc:`Index <fixpt>`): T

Returns the element at the given index 


*********

.. parsed-literal::

  :maroon:`def` flatten(): :doc:`forgearray`\[T\]




*********

.. parsed-literal::

  :maroon:`def` length(): :doc:`Index <fixpt>`

Returns the length of this Array


*********

.. parsed-literal::

  :maroon:`def` map(y: T => R): :doc:`forgearray`\[R\]




*********

.. parsed-literal::

  :maroon:`def` mkString(y: :doc:`string`): :doc:`string`




*********

.. parsed-literal::

  :maroon:`def` reduce(y: (T, T) => T)(:maroon:`implicit` ev0: Coll[T]): T




*********

.. parsed-literal::

  :maroon:`def` update(i: :doc:`Index <fixpt>`, x: T): Unit

Updates the array at the given index 


*********

.. parsed-literal::

  :maroon:`def` zip(y: :doc:`forgearray`\[S\])(z: (T, S) => R): :doc:`forgearray`\[R\]




*********

.. parsed-literal::

  :maroon:`def` zipWithIndex(): :doc:`forgearray`\[:doc:`tup2`\[T,:doc:`Index <fixpt>`\]\]




