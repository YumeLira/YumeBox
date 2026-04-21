package main

import "C"

import (
	"encoding/json"
	"reflect"

	"github.com/metacubex/mihomo/log"
	"gopkg.in/yaml.v3"
)

func marshalJson(obj any) *C.char {
	res, err := json.Marshal(obj)
	if err != nil {
		log.Errorln("marshalJson: %v", err)
		return nil
	}

	return C.CString(string(res))
}

func marshalString(obj any) *C.char {
	if obj == nil {
		return nil
	}

	switch o := obj.(type) {
	case error:
		return C.CString(o.Error())
	case string:
		return C.CString(o)
	}

	log.Errorln("marshalString: invalid type %s", reflect.TypeOf(obj).Name())
	return nil
}

func marshalYaml(obj any) *C.char {
	res, err := yaml.Marshal(obj)
	if err != nil {
		log.Errorln("marshalYaml: %v", err)
		return nil
	}

	return C.CString(string(res))
}
