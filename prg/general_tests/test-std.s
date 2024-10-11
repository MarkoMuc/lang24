.section .data
buff: .word 65

.section .text

.global _start
_start:
	#la a6, buff
	li a6, 0
	sd a6, 0(sp)
	mv a6, sp
	addi sp, sp, 8
	
	sd zero, 0(sp)
	addi sp, sp, 8 # Simulate SL
	
	li a1, 0
	sd a1, 0(sp) # fd = 0
	addi sp, sp, 8
	
	sd a6, 0(sp) # buff = 0(sp)
	addi sp, sp, 8
	
	li a1, 1
	sd a1, 0(sp) # count = 1
	
	addi sp, sp, -24
	
	call _read
	###################
	###################
	addi sp, sp, 8
	li a1, 1
	sd a1, 0(sp) # fd = 1
	addi sp, sp, -8
	
	call _write
	
	ld a2, 0(sp)
	addi sp, sp, 8
	sd a2, 0(sp) 	
	addi sp, sp, -8

	call _exit

