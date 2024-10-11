# Testing putchar and getchar
.section .data
.align 2
BUF: .word 0


.section .text
.align 2
.global _start


_start:
	la s0, BUF
L1:
	call _getchar
	ld a1, 0(sp)
	sd a1, 0(s0)
	
	sd a0, 8(sp)
	call _putint

	li a0, 10
	sd a0, 8(sp)
	call _putchar

	ld a0, 0(s0)
	sd a0, 8(sp)
	call _putchar

	li a0, 10
	sd a0, 8(sp)
	call _putchar
	
	ld a0, 0(s0)

	addi a2, a0, -48
	bltz a2, L2
	addi a2, a0, -57
	bgtz a2, L2
	
	li a2, 10
	sd a2, 0(s0)


L3:	addi a0, zero, 255
L2:	sd a0, 8(sp)
	call _exit

