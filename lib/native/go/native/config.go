package main

//#include "bridge.h"
import "C"

import (
	"runtime"
	"unsafe"

	"cfa/native/config"
)

type remoteValidCallback struct {
	callback unsafe.Pointer
}

func (r *remoteValidCallback) reportStatus(json string) {
	C.fetch_report(r.callback, marshalString(json))
}

//export fetchAndValid
func fetchAndValid(callback unsafe.Pointer, path, url C.c_string, force C.int) {
	go func(path, url string, callback unsafe.Pointer) {
		cb := &remoteValidCallback{callback: callback}

		err := config.FetchAndValid(path, url, force != 0, cb.reportStatus)

		C.fetch_complete(callback, marshalString(err))

		C.release_object(callback)

		runtime.GC()
	}(C.GoString(path), C.GoString(url), callback)
}

//export load
func load(completable unsafe.Pointer, path C.c_string) {
	go func(path string) {
		C.complete(completable, marshalString(config.Load(path)))

		C.release_object(completable)

		runtime.GC()
	}(C.GoString(path))
}

//export loadCompiledConfig
func loadCompiledConfig(completable unsafe.Pointer, path C.c_string) {
	go func(path string) {
		C.complete(completable, marshalString(config.LoadCompiled(path)))

		C.release_object(completable)

		runtime.GC()
	}(C.GoString(path))
}

//export inspectCompiledGroups
func inspectCompiledGroups(yamlText C.c_string, profileDir C.c_string, excludeNotSelectable C.int) *C.char {
	groups, err := config.QueryProxyGroupsFromCompiledYaml(
		C.GoString(yamlText),
		C.GoString(profileDir),
		excludeNotSelectable != 0,
	)
	if err != nil {
		return nil
	}
	return marshalYaml(groups)
}
