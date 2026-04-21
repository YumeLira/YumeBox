package main

//#include "bridge.h"
import "C"

import (
	"context"
	"io"
	"sync"
	"unsafe"

	"golang.org/x/sync/semaphore"

	"cfa/native/app"
	"cfa/native/tun"
)

var rTunLock sync.Mutex
var rTun *remoteTun
var rootTun io.Closer

type remoteTun struct {
	closer   io.Closer
	callback unsafe.Pointer

	closed bool
	limit  *semaphore.Weighted
}

func (t *remoteTun) markSocket(fd int) {
	_ = t.limit.Acquire(context.Background(), 1)
	defer t.limit.Release(1)

	if t.closed {
		return
	}

	C.mark_socket(t.callback, C.int(fd))
}

func (t *remoteTun) querySocketOwner(protocol int, source, target string) string {
	_ = t.limit.Acquire(context.Background(), 1)
	defer t.limit.Release(1)

	if t.closed {
		return "-1\t"
	}

	result := C.query_socket_owner(
		t.callback,
		C.int(protocol),
		C.CString(source),
		C.CString(target),
	)

	if result == nil {
		return "-1\t"
	}

	defer C.free(unsafe.Pointer(result))
	return C.GoString(result)
}

func (t *remoteTun) close() {
	_ = t.limit.Acquire(context.TODO(), 4)
	defer t.limit.Release(4)

	t.closed = true

	if t.closer != nil {
		_ = t.closer.Close()
	}

	app.ApplyTunContext(nil, nil)

	C.release_object(t.callback)
}

func closeCurrentTunLocked() {
	if rTun != nil {
		rTun.close()
		rTun = nil
	}

	if rootTun != nil {
		_ = rootTun.Close()
		rootTun = nil
	}

	app.ApplyTunContext(nil, nil)
}

//export startTun
func startTun(fd C.int, stack, gateway, portal, dns C.c_string, callback unsafe.Pointer) C.int {
	rTunLock.Lock()
	defer rTunLock.Unlock()

	closeCurrentTunLocked()

	f := int(fd)
	s := C.GoString(stack)
	g := C.GoString(gateway)
	p := C.GoString(portal)
	d := C.GoString(dns)

	remote := &remoteTun{callback: callback, closed: false, limit: semaphore.NewWeighted(4)}

	app.ApplyTunContext(remote.markSocket, remote.querySocketOwner)

	closer, err := tun.Start(f, s, g, p, d)
	if err != nil {
		remote.close()

		return 1
	}

	remote.closer = closer

	rTun = remote

	return 0
}

//export stopTun
func stopTun() {
	rTunLock.Lock()
	defer rTunLock.Unlock()

	closeCurrentTunLocked()
}

//export startRootTun
func startRootTun(configYaml C.c_string) *C.char {
	rTunLock.Lock()
	defer rTunLock.Unlock()

	closeCurrentTunLocked()

	closer, err := tun.StartRoot(C.GoString(configYaml))
	if err != nil {
		closeCurrentTunLocked()
		return C.CString(err.Error())
	}

	rootTun = closer
	return nil
}

//export stopRootTun
func stopRootTun() {
	rTunLock.Lock()
	defer rTunLock.Unlock()

	closeCurrentTunLocked()
}
