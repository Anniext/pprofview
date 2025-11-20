package main

import (
	"fmt"
	"time"
)

func main() {
	fmt.Println("程序开始执行...")
	
	// 做一些工作
	for i := 0; i < 10; i++ {
		fmt.Printf("工作中... %d/10\n", i+1)
		doWork()
		time.Sleep(1 * time.Second)
	}
	
	fmt.Println("程序执行完成")
}

func doWork() {
	sum := 0
	for i := 0; i < 1000000; i++ {
		sum += i
	}
}
